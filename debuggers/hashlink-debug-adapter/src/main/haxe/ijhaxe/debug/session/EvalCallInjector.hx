package ijhaxe.debug.session;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.breakpoints.Breakpoints;
import ijhaxe.debug.Pointer;
import ijhaxe.debug.eval.call.CallArg;
import ijhaxe.debug.eval.call.CallTrampoline;
import ijhaxe.debug.eval.call.X64CallEmitter;
import ijhaxe.debug.eval.call.X86CallEmitter;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.target.DebugApi;
import ijhaxe.debug.target.WaitOutcome;
import ijhaxe.debug.values.ValueReader.hex;
import ijhaxe.debug.Trace;

import haxe.Int64;
import haxe.io.Bytes;

/**
	Callbacks into the session for what an injected call cannot own: which INT3
	must stay lifted, where a foreign stop goes, and the state change when the
	debuggee dies mid-call.
**/
typedef EvalCallHooks = {
	// The address of the breakpoint the session is stopped on. The continue
	// machinery keeps its original byte restored until it steps past it, so
	// rearmAll must skip it.
	var keepSuspended:() -> Null<Pointer>;
	// Receives a real pending event from another thread, which owns the process
	// freeze. The session processes it as a normal stop after the command
	// settles, never mid-eval while the inspector's caches are in use.
	var onForeignStop:WaitOutcome -> Void;
	// The debuggee exited mid-call: the session enters its Exited state and
	// releases the dying process.
	var onExited:Int -> Void;
}

/**
	Runs a function inside the stopped debuggee (an eval-call). It writes a
	trampoline over the code at the stopped thread's instruction pointer: a
	short machine-code sequence for the CPU architecture that loads the
	arguments, calls the function and ends in an INT3. The trampoline runs on a
	scratch stack until that INT3; then the original code and the Eip, Esp and
	Rax registers are restored.

	DANGEROUS: this runs arbitrary debuggee code on the session thread, and is
	valid only while stopped. A call that throws, reaches a breakpoint or runs
	longer than CALL_TIMEOUT_MS fails with the state restored.
**/
class EvalCallInjector {
	static inline var CALL_TIMEOUT_MS = 5000;
	static inline var WAIT_POLL_MS = 20;

	final api:DebugApi;
	final debuggeePid:Int;
	final jit:JitInfo;
	final breakpoints:Breakpoints;
	final hooks:EvalCallHooks;

	public function new(api:DebugApi, debuggeePid:Int, jit:JitInfo, breakpoints:Breakpoints, hooks:EvalCallHooks) {
		this.api = api;
		this.debuggeePid = debuggeePid;
		this.jit = jit;
		this.breakpoints = breakpoints;
		this.hooks = hooks;
	}

	/**
		Calls `funcAddr` in the debuggee with `args` (already lowered to raw
		register values) and returns the raw result: RAX, or XMM0 copied into
		RAX for a float return.
	**/
	public function call(threadId:Int, funcAddr:Pointer, args:Array<CallArg>, floatBits:Int):Pointer {
		if (Trace.isEnabled()) {
			Trace.log('[eval-call] call thread=$threadId func=${hex(funcAddr)} args=${args.length}'
				+ ' floatBits=$floatBits ' + describeArgs(args));
		}
		// x86-64 loads argument registers and returns through RAX/XMM0; x86
		// pushes cdecl stack arguments and returns through EAX/ST0. The
		// handshake's bitness picks the emitter.
		var emitter:CallTrampoline = jit.is64 ? new X64CallEmitter(jit.winCall) : new X86CallEmitter();
		var asm = emitter.build(funcAddr, args, floatBits);

		// Lift every planted INT3 for the duration of the call. The called
		// function may throw and catch internally, which trips the hl_throw trap
		// while VM-exception breakpoints are on, or pass a user breakpoint.
		// Either aborts the call and leaves a half-run frame that corrupts later
		// execution. The lift also keeps 0xCC bytes out of the original code the
		// trampoline overwrites and saves.
		breakpoints.suspendAll();
		// Haxe has no `finally`: hold the failure, so the breakpoints are
		// re-planted even when the injection throws
		var error:Null<Dynamic> = null;
		var result:Null<Pointer> = null;
		try {
			result = runInjectedCall(threadId, asm, floatBits);
		} catch (e:Dynamic) {
			error = e;
		}
		// Re-plant the lifted breakpoints, except the one the session is stopped on.
		breakpoints.rearmAll(hooks.keepSuspended());
		if (error != null) {
			if (Trace.isEnabled()) {
				Trace.log('[eval-call] FAILED thread=$threadId func=${hex(funcAddr)}: ' + Std.string(error));
			}
			throw error;
		}
		if (Trace.isEnabled()) {
			Trace.log('[eval-call] done thread=$threadId func=${hex(funcAddr)} raw=${hex(result)}');
		}
		return result;
	}

	static function describeArgs(args:Array<CallArg>):String {
		return "[" + [for (a in args) (a.isFloat ? "f:" : "i:") + hex(a.bits)].join(" ") + "]";
	}

	/**
		Loads Xmm0 on the stopped thread by running a two-instruction injected
		stub. This works around linux, where hl's debug natives cannot write float
		registers: the ptrace write path lacks the FP offsets the read path
		defines. The breakpoints are lifted as in call(). The straight-line stub
		cannot hit one, but the saved original bytes must not contain the
		adapter's 0xCC patches.
	**/
	public function writeXmm0(threadId:Int, bits:Pointer):Void {
		breakpoints.suspendAll();
		var error:Null<Dynamic> = null;

		try {
			runInjectedCall(threadId, X64CallEmitter.buildXmm0Load(bits), 0);
		} catch (e:Dynamic) {
			error = e;
		}
		breakpoints.rearmAll(hooks.keepSuspended());
		if (error != null) {
			throw error;
		}
	}

	// The core of an eval-call: writes the trampoline over the stopped thread's
	// Eip, runs it on a scratch stack to its trailing INT3, and restores the
	// original code and the Eax, Eip and Esp registers. The order of steps makes
	// it exception-safe: every throw happens either before any state changes or
	// after everything is restored.
	function runInjectedCall(threadId:Int, asm:Bytes, floatBits:Int):Pointer {
		var asmSize = asm.length;

		var prevEax = api.readRegister(debuggeePid, threadId, Eax);
		var prevEip = api.readRegister(debuggeePid, threadId, Eip);
		var prevEsp = api.readRegister(debuggeePid, threadId, Esp);

		// registers the injection does not restore; the snapshot lets the trace
		// show any the call clobbered
		var prevEbp = api.readRegister(debuggeePid, threadId, Ebp);
		var prevFlags = api.readRegister(debuggeePid, threadId, EFlags);
		var prevXmm0 = api.readRegister(debuggeePid, threadId, Xmm0);

		if (Trace.isEnabled()) {
			Trace.log('[eval-call] inject thread=$threadId eip=${hex(prevEip)} esp=${hex(prevEsp)}'
				+ ' ebp=${hex(prevEbp)} eax=${hex(prevEax)} flags=${hex(prevFlags)} xmm0=${hex(prevXmm0)}'
				+ ' asm=$asmSize scratch=${hex(scratchStackTop(prevEsp))}');
		}

		var original = Bytes.alloc(asmSize);
		if (!api.readMemory(debuggeePid, prevEip, original, asmSize)) {
			throw new DebugError("Cannot read code to inject a call");
		}
		if (!api.writeMemory(debuggeePid, prevEip, asm, asmSize)) {
			throw new DebugError("Cannot inject the call trampoline");
		}
		api.flush(debuggeePid, prevEip, asmSize);

		// give the call a fresh scratch stack below the current frame
		api.writeRegister(debuggeePid, threadId, Esp, scratchStackTop(prevEsp));

		var trapEnd = Int64.add(prevEip, Int64.ofInt(asmSize)); // Eip AFTER the INT3
		var completed = resumeUntilTrap(threadId, trapEnd);

		api.writeMemory(debuggeePid, prevEip, original, asmSize);
		api.flush(debuggeePid, prevEip, asmSize);

		// x86 returns a float on the x87 stack (ST0), which HL's debug register
		// API does not expose; X86CallEmitter spills it into the scratch slot at
		// scratchStackTop. Everything else returns in RAX/EAX; on x86-64 the
		// trampoline copies a float return into RAX.
		var result = (!jit.is64 && floatBits != 0)
			? readFloatSpill(scratchStackTop(prevEsp), floatBits)
			: api.readRegister(debuggeePid, threadId, Eax);
		var landedEip = api.readRegister(debuggeePid, threadId, Eip);

		api.writeRegister(debuggeePid, threadId, Eax, prevEax);
		api.writeRegister(debuggeePid, threadId, Eip, prevEip);
		api.writeRegister(debuggeePid, threadId, Esp, prevEsp);

		if (Trace.isEnabled()) {
			logRestoredContext(threadId, prevEbp, prevFlags, prevXmm0, completed, landedEip, trapEnd);
		}
		if (!completed || !Int64.eq(landedEip, trapEnd)) {
			throw new DebugError("The called function did not return normally (it threw an exception or hit a breakpoint)");
		}
		return result;
	}

	// Traces the registers the injection does not restore. A CLOBBERED register
	// is state the interrupted function resumes with. Ebp, EFlags and Xmm0 are
	// the only such registers the native API can read; the other volatile
	// registers a call may overwrite cannot be observed.
	function logRestoredContext(threadId:Int, prevEbp:Pointer, prevFlags:Pointer, prevXmm0:Pointer,
			completed:Bool, landedEip:Pointer, trapEnd:Pointer):Void {
		var nowEbp = api.readRegister(debuggeePid, threadId, Ebp);
		var nowFlags = api.readRegister(debuggeePid, threadId, EFlags);
		var nowXmm0 = api.readRegister(debuggeePid, threadId, Xmm0);
		var clobbered = [];
		if (!Int64.eq(nowEbp, prevEbp)) {
			clobbered.push('ebp ${hex(prevEbp)}->${hex(nowEbp)}');
		}
		if (!Int64.eq(nowFlags, prevFlags)) {
			clobbered.push('flags ${hex(prevFlags)}->${hex(nowFlags)}');
		}
		if (!Int64.eq(nowXmm0, prevXmm0)) {
			clobbered.push('xmm0 ${hex(prevXmm0)}->${hex(nowXmm0)}');
		}
		Trace.log('[eval-call] restore thread=$threadId completed=$completed landed=${hex(landedEip)}'
			+ ' trapEnd=${hex(trapEnd)}'
			+ (clobbered.length > 0 ? ' CLOBBERED: ' + clobbered.join(", ") : ' context clean'));
	}

	// Resumes the thread and waits until it traps exactly at `trapEnd` (Eip past
	// the injected INT3). Returns false on exit, on a foreign stop or on timeout.
	// A foreign Breakpoint or Error is a real pending event that owns the
	// process freeze. It goes to hooks.onForeignStop, and the session processes
	// it as a normal stop after the eval-call has cleaned up. Resuming past it
	// with the wrong thread id would leave the debuggee frozen.
	function resumeUntilTrap(threadId:Int, trapEnd:Pointer):Bool {
		api.resume(debuggeePid, threadId);
		var budget = CALL_TIMEOUT_MS;
		while (budget > 0) {
			var outcome = api.wait(debuggeePid, WAIT_POLL_MS);
			switch (outcome.result) {
				case Timeout:
					budget -= WAIT_POLL_MS;
				case Breakpoint:
					var eip = api.readRegister(debuggeePid, outcome.threadId, Eip);
					if (outcome.threadId == threadId && Int64.eq(eip, trapEnd)) {
						return true; // our trampoline's INT3
					}
					// a breakpoint fired in some thread during the call
					if (Trace.isEnabled()) {
						Trace.log('[eval-call] foreign breakpoint during call: thread=${outcome.threadId}'
							+ ' eip=${hex(eip)} (call thread=$threadId trapEnd=${hex(trapEnd)})');
					}
					hooks.onForeignStop(outcome);
					return false;
				case SingleStep:
					api.resume(debuggeePid, outcome.threadId);
				case Handled:
					// an auto-continued lifecycle event (a thread was created,
					// exited or renamed during the call): neither the injected trap
					// nor a failure, so keep waiting
				case Exit:
					if (Trace.isEnabled()) {
						Trace.log('[eval-call] debuggee EXITED during call (thread=${outcome.threadId})');
					}
					hooks.onExited(outcome.threadId);
					return false;
				case Error, StackOverflow:
					// an exception mid-call: also a pending event that must be
					// reported as a stop, not silently discarded
					if (Trace.isEnabled()) {
						Trace.log('[eval-call] ${outcome.result} during call: thread=${outcome.threadId}'
							+ ' eip=${hex(api.readRegister(debuggeePid, outcome.threadId, Eip))}');
					}
					hooks.onForeignStop(outcome);
					return false;
			}
		}
		if (Trace.isEnabled()) {
			Trace.log('[eval-call] TIMEOUT after ${CALL_TIMEOUT_MS}ms waiting for the trampoline trap');
		}
		return false;
	}

	// Reads an x87 float result the x86 trampoline spilled into the scratch slot:
	// a qword for an F64 return, a dword (in the low 32 bits) for F32 — matching
	// how the caller decodes the raw bits.
	function readFloatSpill(slot:Pointer, floatBits:Int):Pointer {
		var size = floatBits == 64 ? 8 : 4;
		var buf = Bytes.alloc(size);
		api.readMemory(debuggeePid, slot, buf, size);
		return size == 8 ? Int64.make(buf.getInt32(4), buf.getInt32(0)) : Int64.make(0, buf.getInt32(0));
	}

	// The top of the scratch stack for an injected call: the 256-byte-aligned
	// address 256 to 511 bytes below the current Esp. The gap keeps the
	// interrupted frame intact. The x86 trampoline stores a float return at
	// the top itself, so a top within 8 bytes of Esp would overwrite the
	// frame's live top slot; the gap also clears the 128-byte red zone that
	// SysV code may use below Esp.
	public static function scratchStackTop(esp:Pointer):Pointer {
		var below = Int64.sub(esp, Int64.ofInt(0x100));
		return Int64.sub(below, Int64.ofInt(below.low & 0xFF));
	}
}

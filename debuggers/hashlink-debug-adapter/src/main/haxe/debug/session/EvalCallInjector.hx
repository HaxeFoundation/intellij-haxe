package debug.session;

import debug.DebugError;
import debug.breakpoints.Breakpoints;
import debug.Pointer;
import debug.eval.call.CallArg;
import debug.eval.call.CallTrampoline;
import debug.eval.call.X64CallEmitter;
import debug.eval.call.X86CallEmitter;
import debug.module.JitInfo;
import debug.target.DebugApi;
import debug.target.WaitOutcome;

import haxe.Int64;
import haxe.io.Bytes;

/**
	Hooks back into the session for the pieces an injected call cannot own:
	which INT3 must stay lifted, where to park a foreign stop, and the state
	transition when the debuggee dies mid-call.
**/
typedef EvalCallHooks = {
	// The breakpoint the session is stopped on, whose byte the stop/continue
	// machinery keeps restored until it steps past it — rearmAll must skip it.
	var keepSuspended:() -> Null<Pointer>;
	// A REAL pending event from another thread that owns the process freeze;
	// the session processes it as a normal stop after the command settles
	// (never mid-eval: the inspector's caches are in use).
	var onForeignStop:WaitOutcome -> Void;
	// The debuggee exited mid-call: the session enters its Exited state and
	// releases the dying process.
	var onExited:Int -> Void;
}

/**
	Runs a function inside the STOPPED debuggee (the eval-call): injects a
	CPU-architecture-specific trampoline over the code at the stopped thread's
	instruction pointer, runs it to a trailing INT3 on a scratch stack, then
	restores the original code and the Eip/Esp/Rax registers.

	DANGEROUS: this runs arbitrary debuggee code on the session thread. Only
	valid while stopped; a call that throws, recurses into a breakpoint, or
	runs longer than CALL_TIMEOUT_MS fails with the state restored.
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
		register values) and returns the raw result (RAX, or XMM0-as-RAX for a
		float return).
	**/
	public function call(threadId:Int, funcAddr:Pointer, args:Array<CallArg>, floatBits:Int):Pointer {
		// the trampoline is CPU-architecture-specific: x86-64 loads argument
		// registers and returns through RAX/XMM0; x86 pushes cdecl stack args and
		// returns through EAX/ST0. Selected once from the handshake bitness.
		var emitter:CallTrampoline = jit.is64 ? new X64CallEmitter(jit.winCall) : new X86CallEmitter();
		var asm = emitter.build(funcAddr, args, floatBits);

		// Lift every planted INT3 for the duration of the call: the called function
		// may internally throw/catch (tripping the hl_throw trap when VM-exception
		// breakpoints are on) or run through a user breakpoint — either would abort
		// the call and leave a half-executed frame that corrupts later execution.
		// The lift also keeps the trampoline's saved bytes clean of our 0xCC.
		breakpoints.suspendAll();
		// no `finally` in Haxe: hold a failure so the breakpoints are ALWAYS
		// re-planted, even when the injection itself throws
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
			throw error;
		}
		return result;
	}

	// The inject/run/restore core of an eval-call: writes the trampoline over the
	// stopped thread's Eip, runs it on a scratch stack to its trailing INT3, and
	// restores the original code and the Eax/Eip/Esp registers. Exception-safe by
	// ordering: the throws happen either before any state is modified or after
	// everything is restored.
	function runInjectedCall(threadId:Int, asm:Bytes, floatBits:Int):Pointer {
		var asmSize = asm.length;
		var prevEax = api.readRegister(debuggeePid, threadId, Eax);
		var prevEip = api.readRegister(debuggeePid, threadId, Eip);
		var prevEsp = api.readRegister(debuggeePid, threadId, Esp);

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
		// API does not expose; X86CallEmitter spilled it into the scratch slot at
		// scratchStackTop, so read it from there. Everything else returns in
		// RAX/EAX (a float return on x86-64 was copied into RAX by the trampoline).
		var result = (!jit.is64 && floatBits != 0)
			? readFloatSpill(scratchStackTop(prevEsp), floatBits)
			: api.readRegister(debuggeePid, threadId, Eax);
		var landedEip = api.readRegister(debuggeePid, threadId, Eip);

		api.writeRegister(debuggeePid, threadId, Eax, prevEax);
		api.writeRegister(debuggeePid, threadId, Eip, prevEip);
		api.writeRegister(debuggeePid, threadId, Esp, prevEsp);

		if (!completed || !Int64.eq(landedEip, trapEnd)) {
			throw new DebugError("The called function did not return normally (it threw an exception or hit a breakpoint)");
		}
		return result;
	}

	// Resume the thread and wait until it traps at exactly `trapEnd` (Eip past
	// our injected INT3). Returns false on exit, a foreign stop, or timeout.
	// A foreign Breakpoint/Error is a REAL pending event that owns the process
	// freeze: it is handed to hooks.onForeignStop for the session to process as
	// a normal stop once the eval teardown is done — resuming past it with the
	// wrong thread id would leave the debuggee frozen forever.
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
					hooks.onForeignStop(outcome);
					return false;
				case SingleStep:
					api.resume(debuggeePid, outcome.threadId);
				case Handled:
					// auto-continued lifecycle event (another thread created/
					// exited/named itself during the call): not our trap and not
					// a failure — keep waiting
				case Exit:
					hooks.onExited(outcome.threadId);
					return false;
				case Error, StackOverflow:
					// an exception mid-call: also a pending event that must be
					// reported as a stop, not silently discarded
					hooks.onForeignStop(outcome);
					return false;
			}
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

	// The scratch stack top for an injected call: the 256-byte-aligned address at
	// or just below the current Esp (matches hld). Running the call here keeps it
	// from corrupting the interrupted frame below Esp.
	static inline function scratchStackTop(esp:Pointer):Pointer {
		var top = Int64.sub(esp, Int64.ofInt(0xFF));
		var lowByte = top.low & 0xFF;
		return Int64.add(top, Int64.ofInt((0x100 - lowByte) & 0xFF));
	}
}

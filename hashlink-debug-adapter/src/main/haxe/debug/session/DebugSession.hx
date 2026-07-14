package debug.session;
import haxe.io.Bytes;
import haxe.io.BytesBuffer;
import haxe.io.BytesInput;
import haxe.io.FPHelper;
import debug.DebugErrorCode;
import debug.Trace;
import dap.protocol.Breakpoint;

import debug.DebugError;
import debug.Pointer;
import debug.eval.call.CallEmitter;
import debug.eval.call.CallEmitter.CallArg;
import debug.layout.Align;
import debug.module.CodeGraph;
import debug.module.ExceptionSites;
import debug.module.TryRegions;
import debug.module.JitInfo;
import debug.module.JitInfoReader;
import debug.module.ModuleDebugInfo;
import debug.target.DebugApi;
import debug.target.DebuggeeProcess;
import debug.target.MemoryReader;
import debug.target.MemoryWriter;
import debug.target.StackWalker;
import debug.target.ThreadInfo;
import debug.target.ThreadRegistry;
import debug.target.WaitOutcome;
import debug.inspect.CpuRegisters;
import debug.inspect.VariableInspector;

import haxe.Int64;
import sys.net.Host;
import sys.net.Socket;
import sys.thread.Deque;
import sys.thread.Thread;

private enum State {
	NotStarted;
	Configured; // attached, breakpoints patchable, debuggee still held on the handshake socket
	Running;
	Stopped(threadId:Int);
	Exited;
}

/**
 * Owns the whole debug session on a single dedicated thread — the only thread
 * that touches DebugApi (required on Windows, where WaitForDebugEvent must run
 * on the attaching thread). Commands come in on a Deque; results and stop/exit
 * events go out through the `emit` callback.
 *
 * All memory/breakpoint logic is factored into testable helpers (JitInfoReader,
 * ModuleDebugInfo, Breakpoints, StackWalker); this class sequences them and the
 * OS event loop.
 */
class DebugSession {

	// After attaching, Windows delivers a burst of startup debug events (initial
	// breakpoint, module/DLL loads, thread creation). drainAttachEvents resumes
	// past each until a wait Timeout says the queue is empty; this bounds how many
	// it will clear so an unexpected stream of events cannot loop it forever.
	static inline var MAX_ATTACH_DRAIN_EVENTS = 20;
	// A forced break (user pause) is requested now but delivered asynchronously,
	// possibly behind a few benign auto-continued events. forceBreakAndDrain polls
	// for it, treating a wait Timeout as "not arrived yet"; this bounds the polling
	// at MAX_FORCE_BREAK_POLLS * ATTACH_DRAIN_MS (~1s) before giving up.
	static inline var MAX_FORCE_BREAK_POLLS = 20;

	static inline var TRAP_FLAG = 0x100;
	static inline var WAIT_POLL_MS = 20;
	static inline var ATTACH_DRAIN_MS = 50;
	static inline var CONNECT_RETRIES = 60;
	static inline var CONNECT_DELAY_MS = 50;
	static inline var HANDSHAKE_READ_TIMEOUT_S = 0.5;

	final api:DebugApi;
	final commands = new Deque<SessionCommand>();
	final emit:DebugEvent->Void;

	var state:State = NotStarted;
	// In launch mode the adapter spawns and owns the debuggee (`process` set);
	// in attach mode the client spawned it and `process` stays null. All debug
	// natives key on the pid, so everything downstream uses `debuggeePid`.
	var process:DebuggeeProcess;
	var debuggeePid:Int = 0;
	var jit:JitInfo;
	var module:ModuleDebugInfo;
	var breakpoints:Breakpoints;
	// Throw-site enumerator + static try-region analysis (both built once
	// jit+module are ready). Which exception modes are active: "all" breaks on
	// every throw; "uncaught" only when no live `try` will catch the throw.
	var exceptionSites:ExceptionSites;
	var tryRegions:TryRegions;
	var exceptionBreakAll:Bool = false;
	var exceptionBreakUncaught:Bool = false;
	// FQNs (or simple names) of exception classes to stop on — the per-type filter.
	var exceptionBreakTypes:Array<String> = [];
	var handshakeSocket:Socket;
	var stackWalker:StackWalker;
	var threadRegistry:ThreadRegistry;
	var stoppedThreadId:Int = 0;
	var currentStoppedBreakpoint:PatchedBreakpoint;
	// A user pause holds the debug event of the thread the forced break landed on
	// (on Windows a transient system thread, NOT a real HL thread). That exact
	// thread must be the one continued to unfreeze the process, so it is stashed
	// here for the next resume while a real HL thread is reported for inspection.
	// -1 when no pause event is held.
	var pauseEventThread:Int = -1;
	var alive:Bool = true;
	// The in-flight step (temps planted, landing pending), bound to its thread;
	// null when no step is active. See ActiveStep for why this is a singleton.
	var activeStep:Null<ActiveStep> = null;
	// A pending debug event from another thread that interrupted an eval-call:
	// it owns the process freeze and must be processed as a normal stop once
	// the current command finishes (processing it mid-eval would re-enter the
	// inspector while its caches are in use).
	var pendingForeignStop:Null<WaitOutcome> = null;
	// variable inspection (created at launch, once jit/module are available);
	// owns the per-stop frame cache + variablesReference registry
	var inspector:VariableInspector;

	public function new(api:DebugApi, emit:DebugEvent->Void) {
		this.api = api;
		this.emit = emit;
	}

	/** Starts the session thread. */
	public function start():Void {
		Thread.create(loop);
	}

	/** Queues a command for the session thread. */
	public function send(command:SessionCommand):Void {
		commands.add(command);
	}

	static inline function dbg(message:String):Void {
		Trace.log(message);
	}

	function loop():Void {
		while (alive) {
			switch (state) {
				case Running:
					pollWhileRunning();
				default:
					handleCommand(commands.pop(true));
			}
		}
		dbg("session loop ended");
	}

	function pollWhileRunning():Void {
		var outcome = api.wait(debuggeePid, WAIT_POLL_MS);
		handleWaitOutcome(outcome);
		// interleave one pending command so setBreakpoints/continue/disconnect are responsive
		var command = commands.pop(false);
		if (command != null) {
			handleCommand(command);
		}
	}

	// A handler bug must never kill the session thread: one dead thread means
	// every subsequent request times out and the client's views go permanently
	// blank. Errors reject the one command and the loop lives on.
	function handleCommand(command:SessionCommand):Void {
		try {
			dispatchCommand(command);
		} catch (e:DebugError) {
			dbg("cmd " + Type.enumConstructor(command) + " failed: " + e.message);
			rejectError(seqOf(command), e);
		} catch (e:Dynamic) {
			dbg("cmd " + Type.enumConstructor(command) + " failed: " + Std.string(e));
			reject(seqOf(command), "Internal debugger error: " + Std.string(e));
		}
		// an eval-call may have been interrupted by another thread's stop; that
		// event owns the process freeze and is processed only now, after the
		// command settled (never mid-eval: the inspector's caches were in use)
		var foreign = pendingForeignStop;
		if (foreign != null) {
			pendingForeignStop = null;
			handleWaitOutcome(foreign);
		}
	}

	// Reject a request with a DebugError's machine-readable code + variables (the
	// DAP Message.id / Message.variables the client branches on).
	inline function rejectError(requestSeq:Int, e:DebugError):Void {
		emit(EvRejected(requestSeq, e.message, e.code, e.variables));
	}

	// Reject a request with a plain, generic-coded message (no structured detail).
	inline function reject(requestSeq:Int, message:String):Void {
		emit(EvRejected(requestSeq, message, DebugErrorCode.Generic, null));
	}

	static function seqOf(command:SessionCommand):Int {
		return switch (command) {
			case CmdLaunch(seq, _): seq;
			case CmdSetBreakpoints(seq, _, _, _, _): seq;
			case CmdConfigurationDone(seq): seq;
			case CmdContinue(seq, _): seq;
			case CmdStep(seq, _, _): seq;
			case CmdPause(seq, _): seq;
			case CmdSetExceptionBreakpoints(seq, _, _): seq;
			case CmdThreads(seq): seq;
			case CmdStackTrace(seq, _): seq;
			case CmdScopes(seq, _): seq;
			case CmdVariables(seq, _): seq;
			case CmdSetVariable(seq, _, _, _): seq;
			case CmdEvaluate(seq, _, _): seq;
			case CmdDisconnect(seq): seq;
		}
	}

	function dispatchCommand(command:SessionCommand):Void {
		dbg("cmd " + Type.enumConstructor(command));
		switch (command) {
			case CmdLaunch(seq, config):
				handleLaunch(seq, config);
			case CmdSetBreakpoints(seq, sourceKey, sourcePath, requested, isReverify):
				handleSetBreakpoints(seq, sourceKey, sourcePath, requested, isReverify);
			case CmdConfigurationDone(seq):
				handleConfigurationDone(seq);
			case CmdContinue(seq, threadId):
				handleContinue(seq, threadId);
			case CmdStep(seq, threadId, mode):
				handleStep(seq, threadId, mode);
			case CmdPause(seq, threadId):
				handlePause(seq, threadId);
			case CmdSetExceptionBreakpoints(seq, filters, filterTypes):
				handleSetExceptionBreakpoints(seq, filters, filterTypes);
			case CmdThreads(seq):
				handleThreads(seq);
			case CmdStackTrace(seq, threadId):
				handleStackTrace(seq, threadId);
			case CmdScopes(seq, frameId):
				handleScopes(seq, frameId);
			case CmdVariables(seq, reference):
				handleVariables(seq, reference);
			case CmdSetVariable(seq, reference, name, value):
				handleSetVariable(seq, reference, name, value);
			case CmdEvaluate(seq, frameId, expression):
				handleEvaluate(seq, frameId, expression);
			case CmdDisconnect(seq):
				handleDisconnect(seq);
		}
	}

	// --- launch / attach ---

	function handleLaunch(requestSeq:Int, config:LaunchConfig):Void {
		try {
			if (!sys.FileSystem.exists(config.program)) {
				throw new DebugError('Program not found: ${config.program}');
			}
			module = new ModuleDebugInfo(config.program);

			if (config.attachPid != null) {
				// Attach mode: the client spawned `hl --debug <port> --debug-wait`
				// itself and owns the process's stdio and lifetime. Spawning from
				// here would go through HL's process.c, which forces SW_HIDE onto
				// the debuggee's first window on Windows (see docs/README.md).
				debuggeePid = config.attachPid;
				handshakeSocket = connectWithRetries(config.debugPort);
			} else {
				var port = DebuggeeProcess.findFreePort();
				process = new DebuggeeProcess(config.hlPath, config.program, config.args, config.cwd, port,
					(category, text) -> emit(EvOutput(category, text)));
				process.startOutputPumps();
				debuggeePid = process.pid;
				handshakeSocket = connectWithRetries(port);
			}
			// The VM sends the whole handshake then blocks on the socket; drain it
			// fully into memory (a read timeout marks the end) and parse from there.
			// Parsing straight off the socket deadlocks against the two HL processes'
			// send/recv buffering, and byte-at-a-time socket reads are far too slow.
			jit = JitInfoReader.read(new BytesInput(readHandshake()));

			if (!api.start(debuggeePid)) {
				throw new DebugError("Failed to attach to the debuggee process");
			}
			drainAttachEvents();

			breakpoints = new Breakpoints(api, debuggeePid);
			exceptionSites = new ExceptionSites(module, jit);
			tryRegions = new TryRegions(module);
			stackWalker = new StackWalker(api, debuggeePid, jit);
			var memReader = new MemoryReader(api, debuggeePid, jit.is64);
			threadRegistry = new ThreadRegistry(memReader,
				new Align(jit.is64, jit.boolSize4), jit.hlVersionMajor, jit.hlVersionMinor);
			inspector = new VariableInspector(module, jit, memReader);
			inspector.frameWalker = tid -> stackWalker.walk(tid);
			var cpuRegisters = new CpuRegisters(api, debuggeePid);
			// CPU registers are only readable while stopped; the callback is invoked
			// for a stopped thread's top frame, but guard defensively.
			inspector.cpuRegistersFor = tid -> state.match(Stopped(_)) ? cpuRegisters.rows(tid) : [];
			inspector.enableWrites(new MemoryWriter(api, debuggeePid, jit.is64));
			inspector.xmm0Writer = value ->
				api.writeRegister(debuggeePid, stoppedThreadId, Xmm0, FPHelper.doubleToI64(value));
			inspector.warnSink = text -> emit(EvOutput("console", text));
			inspector.functionCaller = (funcAddr, args, floatReturn) ->
				callInDebuggee(stoppedThreadId, funcAddr, args, floatReturn);
			applyExceptionBreakpoints(); // honour a pre-launch setExceptionBreakpoints
			state = Configured;
			emit(EvLaunched(requestSeq));
		} catch (e:DebugError) {
			cleanupAfterFailure();
			emit(EvLaunchFailed(requestSeq, e.message));
		} catch (e:Dynamic) {
			cleanupAfterFailure();
			emit(EvLaunchFailed(requestSeq, Std.string(e)));
		}
	}

	// Reads the entire handshake the VM sends before it blocks. Uses a read
	// timeout: once the VM has sent everything and is waiting on us, the next
	// read blocks and throws, which is our end-of-message signal.
	function readHandshake():Bytes {
		handshakeSocket.setTimeout(HANDSHAKE_READ_TIMEOUT_S);
		var accumulated = new BytesBuffer();
		var buffer = Bytes.alloc(8192);
		try {
			while (true) {
				var read = handshakeSocket.input.readBytes(buffer, 0, buffer.length);
				if (read <= 0) {
					break;
				}
				accumulated.addBytes(buffer, 0, read);
			}
		} catch (e:Dynamic) {
			// timeout (Blocked) or Eof: the VM has sent the whole handshake
		}
		var bytes = accumulated.getBytes();
		if (bytes.length == 0) {
			throw new DebugError("The debuggee sent no debug handshake");
		}
		return bytes;
	}

	function connectWithRetries(port:Int):Socket {
		var lastError:Dynamic = null;
		for (_ in 0...CONNECT_RETRIES) {
			try {
				var socket = new Socket();
				socket.connect(new Host("127.0.0.1"), port);
				return socket;
			} catch (e:Dynamic) {
				lastError = e;
				Sys.sleep(CONNECT_DELAY_MS / 1000);
			}
		}
		throw new DebugError("Could not connect to the debuggee debug port: " + Std.string(lastError));
	}

	// Consume the events the OS raises at attach time (e.g. the Windows attach
	// breakpoint / module-load events) so the debuggee is back to a clean state.
	function drainAttachEvents():Void {
		for (_ in 0...MAX_ATTACH_DRAIN_EVENTS) {
			var outcome = api.wait(debuggeePid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case Timeout:
					return;
				case Exit:
					state = Exited;
					releaseExitedProcess(outcome.threadId);
					emit(EvExited(safeExitCode()));
					return;
				default:
					api.resume(debuggeePid, outcome.threadId);
			}
		}
	}

	// --- breakpoints ---

	function handleSetBreakpoints(requestSeq:Int, sourceKey:String, sourcePath:String, requested:Array<RequestedBreakpoint>, isReverify:Bool):Void {
		if (module == null) {
			// not launched yet; cannot resolve. Should not happen (dispatcher buffers), but stay safe.
			var pending = [for (r in requested) unresolved(r, sourcePath)];
			emitBreakpointResults(requestSeq, pending, isReverify);
			return;
		}

		var locations:Array<BreakpointLocation> = [];
		var results:Array<BreakpointResult> = [];
		for (request in requested) {
			var resolved = module.resolveLine(sourcePath, request.line);
			if (resolved.length == 0) {
				results.push(unresolved(request, sourcePath));
			} else {
				for (location in resolved) {
					locations.push({
						id: request.id,
						address: jit.addressOf(location.fidx, location.op),
						fidx: location.fidx, op: location.op, file: sourcePath, line: location.line,
						condition: request.condition
					});
				}
				results.push({id: request.id, verified: true, line: resolved[0].line, sourcePath: sourcePath});
			}
		}

		// while the debuggee runs we must stop it before writing its memory
		var wasRunning = switch (state) { case Running: true; default: false; };
		if (wasRunning) {
			pauseForMemoryWrite();
		}
		breakpoints.setForSource(sourceKey, locations);
		// setForSource re-armed this source's breakpoints; if we are stopped on one of
		// them it just re-planted its INT3 at the current instruction pointer. Lift that
		// INT3 again (keep it suspended) and re-point currentStoppedBreakpoint to the
		// re-installed instance, so the next continue single-steps the real instruction
		// instead of stepping straight into the fresh INT3 and re-hitting the same line
		// (run-to-cursor, or toggling a breakpoint in this file while stopped).
		reconcileStoppedBreakpoint();
		if (wasRunning) {
			resumeAfterMemoryWrite();
		}

		emitBreakpointResults(requestSeq, results, isReverify);
	}

	// Keeps the breakpoint we are currently stopped on suspended (INT3 lifted) across a
	// setForSource re-install. No-op when running, or when the stopped breakpoint is not
	// an address-keyed line breakpoint (e.g. an exception breakpoint) or was removed.
	function reconcileStoppedBreakpoint():Void {
		if (currentStoppedBreakpoint == null) {
			return;
		}
		var reinstalled = breakpoints.atAddress(currentStoppedBreakpoint.address);
		if (reinstalled != null) {
			breakpoints.suspend(reinstalled);
			currentStoppedBreakpoint = reinstalled;
		}
	}

	function emitBreakpointResults(requestSeq:Int, results:Array<BreakpointResult>, isReverify:Bool):Void {
		if (isReverify) {
			for (result in results) {
				emit(EvBreakpointChanged(result));
			}
		} else {
			emit(EvBreakpoints(requestSeq, results));
		}
	}

	function unresolved(request:RequestedBreakpoint, sourcePath:String):BreakpointResult {
		return {id: request.id, verified: false, line: request.line, message: "no executable code at this line", sourcePath: sourcePath};
	}

	// Force the running debuggee to stop, so its memory can be patched, then keep
	// running. Any breakpoint/step event that arrives here is not user-visible.
	function pauseForMemoryWrite():Void {
		var outcome = forceBreakAndDrain();
		if (outcome != null) {
			stoppedThreadId = outcome.threadId;
		}
	}

	// Interrupt the running debuggee (forceBreak) and drain auto-continued
	// lifecycle events until the forced stop lands, returning that stopping
	// outcome. Returns null when the debuggee exited during the interrupt
	// (state=Exited, EvExited emitted) or the stop never arrived in the drain
	// budget. Shared by the silent memory-write pause and the user pause.
	function forceBreakAndDrain():Null<WaitOutcome> {
		api.forceBreak(debuggeePid);
		for (_ in 0...MAX_FORCE_BREAK_POLLS) {
			var outcome = api.wait(debuggeePid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case Timeout: // keep waiting for the forced stop
				case Handled:
					// auto-continued lifecycle event (no thread is frozen by it):
					// NOT the forced stop, keep waiting
				case Exit:
					state = Exited;
					releaseExitedProcess(outcome.threadId);
					emit(EvExited(safeExitCode()));
					return null;
				default:
					return outcome;
			}
		}
		return null;
	}

	function resumeAfterMemoryWrite():Void {
		api.resume(debuggeePid, stoppedThreadId);
	}

	// --- eval-call: run a function inside the stopped debuggee ---

	static inline var CALL_TIMEOUT_MS = 5000;

	/**
	 * Calls `funcAddr` in the debuggee with `args` (already lowered to raw
	 * register values) and returns the raw result (RAX, or XMM0-as-RAX for a
	 * float return). Injects a trampoline over the code at the stopped thread's
	 * instruction pointer, runs it to a trailing INT3, then restores the
	 * original code and the Eip/Esp/Rax registers.
	 *
	 * DANGEROUS: this runs arbitrary debuggee code on the session thread. Only
	 * valid while stopped; a call that throws, recurses into a breakpoint, or
	 * runs longer than CALL_TIMEOUT_MS fails with the state restored.
	 */
	function callInDebuggee(threadId:Int, funcAddr:Pointer, args:Array<CallArg>, floatReturn:Bool):Pointer {
		var asm = new CallEmitter(jit.winCall).build(funcAddr, args, floatReturn);
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

		var result = api.readRegister(debuggeePid, threadId, Eax);
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
	// freeze: it is stashed in `pendingForeignStop` for handleCommand to process
	// as a normal stop once the eval teardown is done — resuming past it with
	// the wrong thread id would leave the debuggee frozen forever.
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
					pendingForeignStop = outcome;
					return false;
				case SingleStep:
					api.resume(debuggeePid, outcome.threadId);
				case Handled:
					// auto-continued lifecycle event (another thread created/
					// exited/named itself during the call): not our trap and not
					// a failure — keep waiting
				case Exit:
					state = Exited;
					releaseExitedProcess(outcome.threadId);
					return false;
				case Error, StackOverflow:
					// an exception mid-call: also a pending event that must be
					// reported as a stop, not silently discarded
					pendingForeignStop = outcome;
					return false;
			}
		}
		return false;
	}

	// The scratch stack top for an injected call: the 256-byte-aligned address at
	// or just below the current Esp (matches hld). Running the call here keeps it
	// from corrupting the interrupted frame below Esp.
	static inline function scratchStackTop(esp:Pointer):Pointer {
		var top = Int64.sub(esp, Int64.ofInt(0xFF));
		var lowByte = top.low & 0xFF;
		return Int64.add(top, Int64.ofInt((0x100 - lowByte) & 0xFF));
	}

	// --- run control ---

	function handleConfigurationDone(requestSeq:Int):Void {
		emit(EvConfigurationDone(requestSeq));
		// release the debuggee: closing the handshake socket unblocks the VM's
		// 1-byte recv so it starts running user code
		if (handshakeSocket != null) {
			try {
				handshakeSocket.close();
			} catch (e:Dynamic) {}
			handshakeSocket = null;
		}
		if (state == Configured) {
			state = Running;
		}
	}

	function handleContinue(requestSeq:Int, threadId:Int):Void {
		switch (state) {
			case Stopped(_):
				var interrupted = stepOverAndResume(threadId);
				state = Running;
				emit(EvContinued(requestSeq));
				if (interrupted != null) {
					// another thread stopped us during the resume dance: report
					// that stop right after the continue response
					handleWaitOutcome(interrupted);
				}
			default:
				reject(requestSeq, "Cannot continue: debuggee is not stopped");
		}
	}

	// Re-execute the original instruction under the trap flag, re-arm the INT3,
	// then let the debuggee run. Returns a pending debug event when one from
	// ANOTHER thread interrupted the dance (all threads run once the pending
	// event is continued, so e.g. a second thread can hit a breakpoint right
	// here) — the caller must process it as a normal stop AFTER settling its
	// own state; we must NOT resume past it or the whole process stays frozen.
	function stepOverAndResume(threadId:Int):Null<WaitOutcome> {
		// resuming invalidates the stopped-frame cache and its variablesReferences
		inspector.invalidate();
		var bp = currentStoppedBreakpoint;
		if (bp != null) {
			var interrupted = trapDance(threadId);
			breakpoints.rearm(bp);
			currentStoppedBreakpoint = null;
			if (state == Exited) {
				return null; // the debuggee exited during the single step
			}
			if (interrupted != null) {
				return interrupted; // a pending event owns the freeze; don't resume
			}
		}
		// A user pause parked on a system break thread whose event must be the one
		// continued; the inspected Haxe thread is not it. Consume it once.
		var resumeThread = pauseEventThread != -1 ? pauseEventThread : threadId;
		pauseEventThread = -1;
		api.resume(debuggeePid, resumeThread);
		return null;
	}

	// User pause: interrupt the running debuggee and report a stop with reason
	// "pause", WITHOUT resuming. The forced break lands on a thread that may not be
	// a real HL thread (a system break thread on Windows), so its event is stashed
	// in pauseEventThread for the resume while a real HL thread is reported for
	// inspection — the user sees a Haxe stack, and continue/step resume correctly.
	function handlePause(requestSeq:Int, threadId:Int):Void {
		switch (state) {
			case Running:
				var outcome = forceBreakAndDrain();
				if (state == Exited) {
					// the debuggee exited during the interrupt (EvExited already sent)
					emit(EvPaused(requestSeq));
					return;
				}
				if (outcome == null) {
					reject(requestSeq, "Could not pause the debuggee");
					return;
				}
				pauseEventThread = outcome.threadId;
				var inspectThread = pauseInspectThread(threadId, outcome.threadId);
				enterStopped(inspectThread, null);
				emit(EvPaused(requestSeq));      // ack the pause request first
				emit(EvStoppedPause(inspectThread)); // then the stopped(reason:"pause") event
			case Stopped(_):
				// already stopped (e.g. a breakpoint hit as the pause arrived): the
				// client is already showing a stop, so just acknowledge
				emit(EvPaused(requestSeq));
			default:
				reject(requestSeq, "Cannot pause: debuggee is not running");
		}
	}

	// The real HL thread to report a pause on: the requested thread when it is a
	// live HL thread, else the first (main) HL thread, else the raw event thread.
	function pauseInspectThread(requested:Int, eventThread:Int):Int {
		var threads = threadList();
		if (requested > 0) {
			for (t in threads) {
				if (t.id == requested) {
					return requested;
				}
			}
		}
		return threads.length > 0 ? threads[0].id : eventThread;
	}

	// --- exception breakpoints (break on any thrown exception) ---

	function handleSetExceptionBreakpoints(requestSeq:Int, filters:Array<String>, filterTypes:Array<String>):Void {
		exceptionBreakAll = filters != null && filters.indexOf("all") >= 0;
		exceptionBreakUncaught = filters != null && filters.indexOf("uncaught") >= 0;
		exceptionBreakTypes = filterTypes != null ? filterTypes : [];
		applyExceptionBreakpoints();
		emit(EvExceptionBreakpointsSet(requestSeq));
	}

	// Reconciles the armed throw-site INT3s with the desired state — armed while
	// either mode ("all" / "uncaught") is on (both plant an INT3 at every throw;
	// the mode only changes whether a hit surfaces). A no-op before launch
	// (breakpoints/sites not built yet — re-run once they are). Arming/disarming
	// writes debuggee memory, so a running debuggee is briefly frozen first.
	function applyExceptionBreakpoints():Void {
		var wanted = exceptionBreakAll || exceptionBreakUncaught || exceptionBreakTypes.length > 0;
		if (breakpoints == null || exceptionSites == null || wanted == breakpoints.isExceptionsArmed()) {
			return;
		}
		var wasRunning = switch (state) { case Running: true; default: false; };
		if (wasRunning) {
			pauseForMemoryWrite();
		}
		if (wanted) {
			breakpoints.armExceptions(exceptionSites.all());
		} else {
			breakpoints.disarmExceptions();
		}
		if (wasRunning) {
			resumeAfterMemoryWrite();
		}
	}

	// True when no live frame's current op sits inside a `try` block — only HL's
	// root handler would catch the throw. Typed catches are approximated as always
	// matching (any active try counts as catching), so this can under-report an
	// uncaught throw whose only enclosing catch has a non-matching type.
	function isUncaught(threadId:Int):Bool {
		for (frame in stackWalker.walk(threadId)) {
			if (tryRegions.isProtected(frame.fidx, frame.op)) {
				return false;
			}
		}
		return true;
	}

	// Describes the value being thrown (the exception in register `reg` of the top
	// frame) for the stopped(reason:"exception") text; a generic message if it
	// can't be read.
	function describeThrow(threadId:Int, reg:Int):String {
		var frames = inspector.framesFor(threadId);
		if (frames.length == 0) {
			return "Exception thrown";
		}
		var value = inspector.readRegisterValue(frames[0].frameId, reg);
		if (value == null || value.value == null) {
			return "Exception thrown";
		}
		return value.type != null ? value.type + ": " + value.value : value.value;
	}

	// True when the thrown value (register `reg` of the throwing frame) is an
	// instance of one of the configured type filters — by FQN or simple name,
	// including subclasses (the tsuper chain). Empty filter set ⇒ false.
	function throwMatchesTypes(threadId:Int, reg:Int):Bool {
		if (exceptionBreakTypes.length == 0) {
			return false;
		}
		var frames = inspector.framesFor(threadId);
		if (frames.length == 0) {
			return false;
		}
		return inspector.registerValueMatchesType(frames[0].frameId, reg, exceptionBreakTypes);
	}

	// The single-step-over-the-patched-instruction sequence. On return the
	// debuggee is frozen again (either our SingleStep event or an interrupting
	// event is pending), which is exactly when register writes are reliable.
	function trapDance(threadId:Int):Null<WaitOutcome> {
		setTrapFlag(threadId);
		api.resume(debuggeePid, threadId);
		var interrupted = waitForSingleStep(threadId);
		// clear the trap flag or the debuggee keeps single-stepping forever
		clearTrapFlag(threadId);
		return interrupted;
	}

	// Waits for OUR thread's single-step to complete. Multithreaded reality:
	// while the step's one instruction runs, every other thread runs too, so
	// arbitrary events can arrive first —
	//  - Handled(4): thread create/exit/set-name etc. that hl_debug_wait ALREADY
	//    continued internally. Not a stop; keep waiting. (Treating these as the
	//    step's completion was the random multithreaded freeze: the early return
	//    cleared the trap flag while threads were RUNNING — unreliable register
	//    writes, stuck TF — and the final resume then targeted the wrong event.)
	//  - Breakpoint/Error/StackOverflow from any thread: a REAL pending event
	//    that now owns the process freeze. Returned to the caller to be handled
	//    as a normal stop; continuing it with our thread id would fail and leave
	//    the debuggee frozen forever.
	function waitForSingleStep(threadId:Int):Null<WaitOutcome> {
		for (_ in 0...100) {
			var outcome = api.wait(debuggeePid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case SingleStep:
					if (outcome.threadId == threadId) {
						return null;
					}
					// another thread's leftover trap: continue it, keep waiting
					api.resume(debuggeePid, outcome.threadId);
				case Handled:
					// already continued inside hl_debug_wait: not a stop
				case Timeout:
					// keep waiting: our instruction hasn't retired yet
				case Exit:
					state = Exited;
					releaseExitedProcess(outcome.threadId);
					emit(EvExited(safeExitCode()));
					return null;
				case Breakpoint, Error, StackOverflow:
					return outcome;
			}
		}
		dbg("waitForSingleStep: no single-step event after 100 polls");
		return null;
	}

	// --- stepping ---

	function handleStep(requestSeq:Int, threadId:Int, mode:StepMode):Void {
		switch (state) {
			case Stopped(_):
				var interrupted = planStep(threadId, mode);
				state = Running;
				emit(EvStepStarted(requestSeq)); // ack now; the stopped(reason:"step") event follows
				if (interrupted != null) {
					// another thread stopped us during the resume dance: report
					// that stop right after the step response
					handleWaitOutcome(interrupted);
				} else if (activeStep == null) {
					// No landing could be planted at all (the only "next" is an
					// unresolvable native return). Behave like continue and tell
					// the client we are running rather than leaving it waiting
					// for a step stop that cannot exist. NOTE: a step whose
					// landings ARE planted waits for them however long the code
					// runs (a slow call is not a reason to give up the step).
					emit(EvResumed(threadId));
				}
			default:
				reject(requestSeq, "Cannot step: debuggee is not stopped");
		}
	}

	// Plant the temporary breakpoints that mark where this step should land, then
	// resume (stepping over the instruction we are parked on). `activeStep`
	// afterwards says whether any landing was planted (null = the caller
	// downgrades the step to a plain continue). Returns a pending debug event
	// when another thread interrupted the resume dance.
	function planStep(threadId:Int, mode:StepMode):Null<WaitOutcome> {
		breakpoints.clearTemps();
		activeStep = null;
		var startEsp = api.readRegister(debuggeePid, threadId, Esp);

		var eip = api.readRegister(debuggeePid, threadId, Eip);
		var position = jit.resolveAddress(eip);
		if (position == null) {
			// not in known bytecode (e.g. inside a native call): can't compute targets
			return stepOverAndResume(threadId);
		}
		var fidx = position.fidx;
		var startLine = module.lineOf(fidx, position.op);
		var graph = new CodeGraph(module.opcodes(fidx));
		var targets = graph.stepTargets(position.op, startLine, (op) -> module.lineOf(fidx, op));
		var returnAddress = currentReturnAddress(threadId);

		if (mode == StepOut) {
			// step out: stop only when the current function returns
			if (returnAddress != null) {
				breakpoints.addTemp(returnAddress);
			}
		} else {
			for (op in targets.lineChangeOps) {
				breakpoints.addTemp(jit.addressOf(fidx, op));
			}
			if (targets.returns && returnAddress != null) {
				breakpoints.addTemp(returnAddress);
			}
			if (mode == StepIn) {
				for (op in targets.callOps) {
					var callee = module.callTargetFunction(fidx, op);
					if (callee >= 0) {
						breakpoints.addTemp(jit.addressOf(callee, 0)); // callee entry = first opcode
					}
				}
			}
		}

		if (breakpoints.hasTemps()) {
			activeStep = {threadId: threadId, mode: mode, startEsp: startEsp};
		}
		return stepOverAndResume(threadId);
	}

	function currentReturnAddress(threadId:Int):Null<Pointer> {
		var frames = stackWalker.walk(threadId);
		return frames.length >= 2 ? frames[1].address : null;
	}

	// The temp we hit is at a deeper (recursive) frame than the step started in:
	// not our landing. Single-step past it, re-arm it, and keep running.
	function stepPastTempAndResume(threadId:Int, address:Pointer):Void {
		breakpoints.suspendTemp(address);
		var interrupted = trapDance(threadId);
		breakpoints.rearmTemp(address);
		if (state == Exited) {
			return;
		}
		if (interrupted != null) {
			// a pending event from another thread owns the freeze: process it as
			// a normal stop instead of resuming past it (bounded reentry — each
			// nested call consumes one already-pending event)
			handleWaitOutcome(interrupted);
			return;
		}
		api.resume(debuggeePid, threadId);
	}

	function finishStep():Void {
		if (breakpoints != null) {
			breakpoints.clearTemps();
		}
		activeStep = null;
	}

	// Enter the Stopped state on `threadId`: end any active step, record the
	// breakpoint we stopped at (null for a step or exception landing), and prime
	// the inspector for a new stop. The caller emits the specific stopped event.
	inline function enterStopped(threadId:Int, breakpoint:Null<PatchedBreakpoint>):Void {
		finishStep();
		currentStoppedBreakpoint = breakpoint;
		stoppedThreadId = threadId;
		inspector.startStop(threadId);
		state = Stopped(threadId);
	}

	// Stack grows down: a shallower-or-equal frame has esp >= the step-start
	// esp. Only meaningful for the step's OWN thread — every thread has its own
	// stack, so comparing another thread's esp against step.startEsp is noise
	// (foreign temp hits are filtered out before this is consulted).
	function frameGuardSatisfied(step:ActiveStep):Bool {
		if (step.mode == StepIn) {
			return true; // any landing (same-frame line change or callee entry) is valid
		}
		var esp = api.readRegister(debuggeePid, step.threadId, Esp);
		return Int64.compare(esp, step.startEsp) >= 0;
	}

	function handleThreads(requestSeq:Int):Void {
		switch (state) {
			case Stopped(_):
				emit(EvThreads(requestSeq, threadList()));
			default:
				// not stopped: report the last-known trigger thread so the client
				// always has at least one thread to attach its views to
				emit(EvThreads(requestSeq, [{id: stoppedThreadId == 0 ? 1 : stoppedThreadId, name: "main"}]));
		}
	}

	// The live threads (from HL's registry), version-adaptive; falls back to a
	// single "main" thread when the program was compiled without thread support.
	function threadList():Array<ThreadInfo> {
		return threadRegistry.read(jit.threadsPtr, jit.threads, stoppedThreadId);
	}

	function handleStackTrace(requestSeq:Int, threadId:Int):Void {
		switch (state) {
			case Stopped(_):
				var frames:Array<FrameInfo> = [];
				for (frame in inspector.framesFor(threadId)) {
					var location = frame.location;
					var source = module.lookup(location.fidx, location.op);
					frames.push({
						id: frame.frameId,
						name: module.functionName(location.fidx),
						file: source != null ? source.file : null,
						line: source != null ? source.line : 0
					});
				}
				emit(EvStackTrace(requestSeq, frames));
			default:
				reject(requestSeq, "Cannot get stack trace: debuggee is not stopped");
		}
	}

	// --- variable inspection (delegated to VariableInspector) ---

	function handleScopes(requestSeq:Int, frameId:Int):Void {
		switch (state) {
			case Stopped(_):
				emit(EvScopes(requestSeq, inspector.scopesFor(frameId)));
			default:
				reject(requestSeq, "Cannot get scopes: debuggee is not stopped");
		}
	}

	function handleEvaluate(requestSeq:Int, frameId:Int, expression:String):Void {
		switch (state) {
			case Stopped(_):
				try {
					emit(EvEvaluated(requestSeq, inspector.evaluate(frameId, expression)));
				} catch (e:DebugError) {
					rejectError(requestSeq, e);
				} catch (e:Dynamic) {
					reject(requestSeq, "Cannot evaluate: " + Std.string(e));
				}
			default:
				reject(requestSeq, "Cannot evaluate: debuggee is not stopped");
		}
	}

	function handleVariables(requestSeq:Int, reference:Int):Void {
		switch (state) {
			case Stopped(_):
				emit(EvVariables(requestSeq, inspector.variablesFor(reference)));
			default:
				reject(requestSeq, "Cannot get variables: debuggee is not stopped");
		}
	}

	function handleSetVariable(requestSeq:Int, reference:Int, name:String, value:String):Void {
		switch (state) {
			case Stopped(_):
				try {
					emit(EvVariableSet(requestSeq, inspector.setVariable(reference, name, value)));
				} catch (e:DebugError) {
					rejectError(requestSeq, e);
				} catch (e:Dynamic) {
					reject(requestSeq, "Cannot set value: " + Std.string(e));
				}
			default:
				reject(requestSeq, "Cannot set a value: debuggee is not stopped");
		}
	}

	// A thread's CPU registers for the inspector's Registers scope. Only the
	// architecture-neutral indexes (0..3 = SP/BP/IP/FLAGS) are read: the only
	// ones hl_debug_read_register maps on every platform (higher indexes are
	// x86-specific and fall back to Rax on Windows). All threads are frozen at a
	// stop, so any thread's registers are readable.
	function handleDisconnect(requestSeq:Int):Void {
		if (debuggeePid != 0) {
			// Free the debuggee before we let go. Launch mode: kill it (we own it).
			// Attach mode: no kill (the client owns its lifetime), so restore every
			// patched INT3 first — the process may keep running after we detach and a
			// leftover trap would crash it. Either way, continue any held stop event so
			// a suspended process does not stall the detach we do further below.
			if (process != null) {
				process.kill();
				dbg("disconnect: kill done");
			} else if (breakpoints != null) {
				try {
					breakpoints.removeAll();
				} catch (e:Dynamic) {}
				dbg("disconnect: breakpoints restored");
			}
			switch (state) {
				case Stopped(threadId):
					try {
						api.resume(debuggeePid, threadId);
					} catch (e:Dynamic) {}
					dbg("disconnect: resumed stopped thread " + threadId);
				default:
			}
			// Drain any still-pending debug events (the just-continued stop, or an
			// EXIT_PROCESS from a concurrent terminate) and continue them. Windows
			// DebugActiveProcessStop stalls while a debug event is outstanding, so a
			// suspended debuggee we detach without draining leaves the client stuck on
			// "waiting for process detach" — it cannot die until we let go cleanly.
			var drained = 0;
			while (drained++ < MAX_ATTACH_DRAIN_EVENTS) {
				var outcome = api.wait(debuggeePid, ATTACH_DRAIN_MS);
				switch (outcome.result) {
					case Timeout, Exit:
						break; // nothing pending, or the debuggee is already gone
					default:
						try {
							api.resume(debuggeePid, outcome.threadId);
						} catch (e:Dynamic) {}
				}
			}
			dbg("disconnect: drained pending events");
		}
		// Answer the disconnect NOW, before the detach. In attach mode
		// DebugActiveProcessStop can stall for seconds on a just-suspended debuggee;
		// the client tears us down (killing both processes) the instant it sees this
		// response — which also unblocks/moots the detach. Waiting for the detach here
		// only made the client sit on its disconnect timeout.
		alive = false;
		emit(EvSessionEnded(requestSeq));
		dbg("disconnect: response emitted");
		if (debuggeePid != 0) {
			try {
				api.stop(debuggeePid);
			} catch (e:Dynamic) {}
			dbg("disconnect: api.stop done");
			if (process != null) {
				process.close();
				dbg("disconnect: close done");
			}
		}
		closeHandshake();
	}

	// --- wait-event classification while running ---

	function handleWaitOutcome(outcome:WaitOutcome):Void {
		if (outcome.result != Timeout) {
			dbg("wait outcome " + outcome.result + " tid=" + outcome.threadId);
		}
		switch (outcome.result) {
			case Timeout:
				// nothing pending
			case Exit:
				finishStep();
				state = Exited;
				releaseExitedProcess(outcome.threadId);
				emit(EvExited(safeExitCode()));
			case Breakpoint:
				handleBreakpointHit(outcome.threadId);
			case SingleStep:
				api.resume(debuggeePid, outcome.threadId);
			case Error, StackOverflow:
				enterStopped(outcome.threadId, null);
				emit(EvStoppedException(outcome.threadId, outcome.result == StackOverflow ? "Stack overflow" : "Unhandled exception"));
			case Handled:
				// hl_debug_wait already continued this event internally (thread
				// create/exit/set-name, dll load, ...). Continuing again is at
				// best a silent failure and at worst blindly continues a REAL
				// event that arrived in the meantime — do nothing.
		}
	}

	function handleBreakpointHit(threadId:Int):Void {
		// INT3 leaves the instruction pointer one byte past the trap
		var eip = api.readRegister(debuggeePid, threadId, Eip);
		var hitAddress = Int64.sub(eip, Int64.ofInt(1));
		var userBp = breakpoints != null ? breakpoints.atAddress(hitAddress) : null;
		var temp = breakpoints != null && breakpoints.isTemp(hitAddress);
		var excEntry = breakpoints != null ? breakpoints.exceptionAt(hitAddress) : null;

		if (userBp == null && !temp && excEntry == null) {
			// attach/loader breakpoint or spurious: just keep going
			api.resume(debuggeePid, threadId);
			return;
		}

		// rewind past the INT3 so the trapped instruction can run on the next resume
		api.writeRegister(debuggeePid, threadId, Eip, hitAddress);

		// a real breakpoint always wins over a step landing
		if (userBp != null) {
			// A conditional breakpoint only stops when its expression is true.
			// Evaluate it against the hitting thread's top frame; a false result
			// resumes without stopping (and WITHOUT ending an in-flight step — the
			// step's temps are still planted, so it keeps progressing).
			if (userBp.condition != null && userBp.condition != "") {
				inspector.startStop(threadId);
				stoppedThreadId = threadId; // the condition's eval-calls target this thread
				if (!breakpointConditionHolds(threadId, userBp)) {
					inspector.invalidate();
					var interrupted = resumePastUserBreakpoint(threadId, userBp);
					if (state == Exited) {
						return;
					}
					if (interrupted != null) {
						handleWaitOutcome(interrupted);
					}
					return;
				}
			}
			breakpoints.suspend(userBp);
			enterStopped(threadId, userBp);
			emit(EvStoppedBreakpoint(threadId, [userBp.id]));
			return;
		}

		// An exception is being thrown here and the exception breakpoint is armed.
		// "all" stops on every throw; "uncaught" stops only when no live `try` will
		// catch it — a caught throw under uncaught-only is resumed past silently
		// (same trap-dance as a false conditional breakpoint), so the catch runs.
		if (excEntry != null) {
			var stop = exceptionBreakAll
				|| (exceptionBreakUncaught && isUncaught(threadId))
				|| throwMatchesTypes(threadId, excEntry.reg);
			if (!stop) {
				inspector.invalidate();
				var interrupted = resumePastUserBreakpoint(threadId, excEntry.bp);
				if (state == Exited) {
					return;
				}
				if (interrupted != null) {
					handleWaitOutcome(interrupted);
				}
				return;
			}
			// Restore the original byte so the throw itself runs on continue; the
			// trap dance (stepOverAndResume) single-steps it and re-arms the site.
			// Reported BEFORE the throw executes, so the frame is the throwing function.
			breakpoints.suspend(excEntry.bp);
			enterStopped(threadId, excEntry.bp);
			emit(EvStoppedException(threadId, describeThrow(threadId, excEntry.reg)));
			return;
		}

		// A temporary (step) breakpoint. Temps live at CODE addresses, so any
		// thread executing that line traps: a hit by a thread that does NOT own
		// the step is never its landing — step that thread past and keep going.
		var step = activeStep;
		if (step != null && threadId != step.threadId) {
			stepPastTempAndResume(threadId, hitAddress);
			return;
		}
		// the owning thread: honour the recursion frame guard for step over/out
		if (step != null && !frameGuardSatisfied(step)) {
			stepPastTempAndResume(threadId, hitAddress);
			return;
		}
		enterStopped(threadId, null);
		emit(EvStoppedStep(threadId));
	}

	// Evaluates a conditional breakpoint against the hitting thread's top frame.
	// FAIL SAFE: any error (bad expression, non-Bool result, no frame) stops the
	// debuggee and reports the reason, so a broken condition is never silently
	// skipped — the user always sees why.
	function breakpointConditionHolds(threadId:Int, bp:PatchedBreakpoint):Bool {
		var frames = inspector.framesFor(threadId);
		if (frames.length == 0) {
			emitConditionNote(bp, "no stack frame to evaluate against");
			return true;
		}
		try {
			return inspector.evaluateBool(frames[0].frameId, bp.condition);
		} catch (e:DebugError) {
			emitConditionNote(bp, e.message);
			return true;
		} catch (e:Dynamic) {
			emitConditionNote(bp, Std.string(e));
			return true;
		}
	}

	function emitConditionNote(bp:PatchedBreakpoint, reason:String):Void {
		emit(EvOutput("console", "[debugger] breakpoint condition \"" + bp.condition + "\" could not be evaluated ("
			+ reason + "); stopping." + String.fromCharCode(10)));
	}

	// Step over a conditional breakpoint whose condition was false and keep
	// running, WITHOUT emitting a stop and without disturbing currentStoppedBreakpoint
	// or an in-flight step. Returns a pending event if another thread interrupted
	// the single-step dance (the caller settles it as a normal stop).
	function resumePastUserBreakpoint(threadId:Int, bp:PatchedBreakpoint):Null<WaitOutcome> {
		breakpoints.suspend(bp);
		var interrupted = trapDance(threadId);
		breakpoints.rearm(bp);
		if (state == Exited) {
			return null;
		}
		if (interrupted != null) {
			return interrupted; // a pending event owns the freeze; don't resume past it
		}
		api.resume(debuggeePid, threadId);
		return null;
	}

	// --- helpers ---

	function setTrapFlag(threadId:Int):Void {
		var flags = Int64.toInt(api.readRegister(debuggeePid, threadId, EFlags));
		api.writeRegister(debuggeePid, threadId, EFlags, Int64.ofInt(flags | TRAP_FLAG));
	}

	function clearTrapFlag(threadId:Int):Void {
		var flags = Int64.toInt(api.readRegister(debuggeePid, threadId, EFlags));
		api.writeRegister(debuggeePid, threadId, EFlags, Int64.ofInt(flags & ~TRAP_FLAG));
	}

	// hl_debug_wait returns Exit for the OS's exit-process debug event WITHOUT
	// continuing it, and Windows keeps the dying process alive until the
	// debugger continues that event and detaches. Release it so whoever owns
	// the process (the IDE/test runner in attach mode, our own Process handle
	// in launch mode) sees it actually terminate. Call BEFORE reading the exit
	// code: waitExitCode blocks on full termination.
	function releaseExitedProcess(threadId:Int):Void {
		try {
			api.resume(debuggeePid, threadId);
		} catch (e:Dynamic) {}
		try {
			api.stop(debuggeePid);
		} catch (e:Dynamic) {}
	}

	function safeExitCode():Int {
		if (process == null) {
			return 0;
		}
		var code = process.tryExitCode();
		return code == null ? process.waitExitCode() : code;
	}

	function cleanupAfterFailure():Void {
		closeHandshake();
		if (debuggeePid != 0) {
			// detach if the attach had already happened; harmless otherwise
			try {
				api.stop(debuggeePid);
			} catch (e:Dynamic) {}
		}
		if (process != null) {
			process.kill();
			process.close();
			process = null;
		}
		debuggeePid = 0;
	}

	function closeHandshake():Void {
		if (handshakeSocket != null) {
			try {
				handshakeSocket.close();
			} catch (e:Dynamic) {}
			handshakeSocket = null;
		}
	}
}

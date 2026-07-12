package debug.session;
import dap.protocol.Breakpoint;

import debug.DebugError;
import debug.Pointer;
import debug.module.CodeGraph;
import debug.module.JitInfo;
import debug.module.JitInfoReader;
import debug.module.ModuleDebugInfo;
import debug.target.DebugApi;
import debug.target.DebuggeeProcess;
import debug.target.MemoryReader;
import debug.target.WaitOutcome;
import debug.target.StackWalker;
import debug.values.VariableInspector;

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
	var handshakeSocket:Socket;
	var stackWalker:StackWalker;
	var threadRegistry:debug.target.ThreadRegistry;
	var stoppedThreadId:Int = 0;
	var currentStoppedBreakpoint:PatchedBreakpoint;
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
		debug.Trace.log(message);
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
			emit(EvRejected(seqOf(command), e.message));
		} catch (e:Dynamic) {
			dbg("cmd " + Type.enumConstructor(command) + " failed: " + Std.string(e));
			emit(EvRejected(seqOf(command), "Internal debugger error: " + Std.string(e)));
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

	static function seqOf(command:SessionCommand):Int {
		return switch (command) {
			case CmdLaunch(seq, _): seq;
			case CmdSetBreakpoints(seq, _, _, _, _): seq;
			case CmdConfigurationDone(seq): seq;
			case CmdContinue(seq, _): seq;
			case CmdStep(seq, _, _): seq;
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
			jit = JitInfoReader.read(new haxe.io.BytesInput(readHandshake()));

			if (!api.start(debuggeePid)) {
				throw new DebugError("Failed to attach to the debuggee process");
			}
			drainAttachEvents();

			breakpoints = new Breakpoints(api, debuggeePid);
			stackWalker = new StackWalker(api, debuggeePid, jit);
			var memReader = new MemoryReader(api, debuggeePid, jit.is64);
			threadRegistry = new debug.target.ThreadRegistry(memReader,
				new debug.layout.Align(jit.is64, jit.boolSize4), jit.hlVersionMajor, jit.hlVersionMinor);
			inspector = new VariableInspector(module, jit, memReader);
			inspector.frameWalker = tid -> stackWalker.walk(tid);
			inspector.cpuRegistersFor = cpuRegisterRows;
			inspector.enableWrites(new debug.target.MemoryWriter(api, debuggeePid, jit.is64));
			inspector.xmm0Writer = value ->
				api.writeRegister(debuggeePid, stoppedThreadId, Xmm0, haxe.io.FPHelper.doubleToI64(value));
			inspector.warnSink = text -> emit(EvOutput("console", text));
			inspector.functionCaller = (funcAddr, args, floatReturn) ->
				callInDebuggee(stoppedThreadId, funcAddr, args, floatReturn);
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
	function readHandshake():haxe.io.Bytes {
		handshakeSocket.setTimeout(HANDSHAKE_READ_TIMEOUT_S);
		var accumulated = new haxe.io.BytesBuffer();
		var buffer = haxe.io.Bytes.alloc(8192);
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
		for (_ in 0...20) {
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

		var locations:Array<{id:Int, address:Pointer, fidx:Int, op:Int, file:String, line:Int}> = [];
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
						fidx: location.fidx, op: location.op, file: sourcePath, line: location.line
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
		if (wasRunning) {
			resumeAfterMemoryWrite();
		}

		emitBreakpointResults(requestSeq, results, isReverify);
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
		api.forceBreak(debuggeePid);
		for (_ in 0...20) {
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
					return;
				default:
					stoppedThreadId = outcome.threadId;
					return;
			}
		}
	}

	function resumeAfterMemoryWrite():Void {
		api.resume(debuggeePid, stoppedThreadId);
	}

	// --- eval-call (M13): run a function inside the stopped debuggee ---

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
	function callInDebuggee(threadId:Int, funcAddr:Pointer, args:Array<debug.eval.CallEmitter.CallArg>, floatReturn:Bool):Pointer {
		var asm = new debug.eval.CallEmitter(jit.winCall).build(funcAddr, args, floatReturn);
		var asmSize = asm.length;

		var prevEax = api.readRegister(debuggeePid, threadId, Eax);
		var prevEip = api.readRegister(debuggeePid, threadId, Eip);
		var prevEsp = api.readRegister(debuggeePid, threadId, Esp);

		var original = haxe.io.Bytes.alloc(asmSize);
		if (!api.readMemory(debuggeePid, prevEip, original, asmSize)) {
			throw new DebugError("Cannot read code to inject a call");
		}
		if (!api.writeMemory(debuggeePid, prevEip, asm, asmSize)) {
			throw new DebugError("Cannot inject the call trampoline");
		}
		api.flush(debuggeePid, prevEip, asmSize);

		// give the call a fresh scratch stack below the current frame, aligned
		// down to a 256-byte boundary (matches hld)
		var stackTop = Int64.sub(prevEsp, Int64.ofInt(0xFF));
		var lowByte = Int64.getLow(stackTop) & 0xFF;
		stackTop = Int64.add(stackTop, Int64.ofInt((0x100 - lowByte) & 0xFF));
		api.writeRegister(debuggeePid, threadId, Esp, stackTop);

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
					// a user breakpoint fired in some thread during the call
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
				emit(EvRejected(requestSeq, "Cannot continue: debuggee is not stopped"));
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
		api.resume(debuggeePid, threadId);
		return null;
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
				emit(EvRejected(requestSeq, "Cannot step: debuggee is not stopped"));
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
	function threadList():Array<debug.target.ThreadInfo> {
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
				emit(EvRejected(requestSeq, "Cannot get stack trace: debuggee is not stopped"));
		}
	}

	// --- variable inspection (delegated to VariableInspector) ---

	function handleScopes(requestSeq:Int, frameId:Int):Void {
		switch (state) {
			case Stopped(_):
				emit(EvScopes(requestSeq, inspector.scopesFor(frameId)));
			default:
				emit(EvRejected(requestSeq, "Cannot get scopes: debuggee is not stopped"));
		}
	}

	function handleEvaluate(requestSeq:Int, frameId:Int, expression:String):Void {
		switch (state) {
			case Stopped(_):
				try {
					emit(EvEvaluated(requestSeq, inspector.evaluate(frameId, expression)));
				} catch (e:DebugError) {
					emit(EvRejected(requestSeq, e.message));
				} catch (e:Dynamic) {
					emit(EvRejected(requestSeq, "Cannot evaluate: " + Std.string(e)));
				}
			default:
				emit(EvRejected(requestSeq, "Cannot evaluate: debuggee is not stopped"));
		}
	}

	function handleVariables(requestSeq:Int, reference:Int):Void {
		switch (state) {
			case Stopped(_):
				emit(EvVariables(requestSeq, inspector.variablesFor(reference)));
			default:
				emit(EvRejected(requestSeq, "Cannot get variables: debuggee is not stopped"));
		}
	}

	function handleSetVariable(requestSeq:Int, reference:Int, name:String, value:String):Void {
		switch (state) {
			case Stopped(_):
				try {
					emit(EvVariableSet(requestSeq, inspector.setVariable(reference, name, value)));
				} catch (e:DebugError) {
					emit(EvRejected(requestSeq, e.message));
				} catch (e:Dynamic) {
					emit(EvRejected(requestSeq, "Cannot set value: " + Std.string(e)));
				}
			default:
				emit(EvRejected(requestSeq, "Cannot set a value: debuggee is not stopped"));
		}
	}

	// A thread's CPU registers for the inspector's Registers scope. Only the
	// architecture-neutral indexes (0..3 = SP/BP/IP/FLAGS) are read: the only
	// ones hl_debug_read_register maps on every platform (higher indexes are
	// x86-specific and fall back to Rax on Windows). All threads are frozen at a
	// stop, so any thread's registers are readable.
	function cpuRegisterRows(threadId:Int):Array<debug.values.VariableInfo> {
		var rows:Array<debug.values.VariableInfo> = [];
		if (state.match(Stopped(_))) {
			try {
				var read = (name, register) -> {
					var value = api.readRegister(debuggeePid, threadId, register);
					rows.push({name: name, value: debug.values.ValueReader.hex(value), type: "CPU", reference: 0});
					value;
				};
				read("SP", Esp);
				read("BP", Ebp);
				read("IP", Eip);
				var flags = Int64.toInt(read("FLAGS", EFlags));
				rows[rows.length - 1].value += flagBits(flags);
			} catch (e:Dynamic) {
				dbg("cpu register read failed: " + Std.string(e));
			}
		}
		return rows;
	}

	static function flagBits(flags:Int):String {
		var names = [];
		if (flags & 0x001 != 0) names.push("CF");
		if (flags & 0x004 != 0) names.push("PF");
		if (flags & 0x040 != 0) names.push("ZF");
		if (flags & 0x080 != 0) names.push("SF");
		if (flags & 0x100 != 0) names.push("TF");
		if (flags & 0x400 != 0) names.push("DF");
		if (flags & 0x800 != 0) names.push("OF");
		return names.length == 0 ? "" : " [" + names.join(" ") + "]";
	}

	function handleDisconnect(requestSeq:Int):Void {
		if (debuggeePid != 0) {
			// Order matters: kill first (works on a suspended process and stops it
			// from reaching further breakpoints), then continue any un-continued
			// debug event so the termination can complete, then detach —
			// DebugActiveProcessStop wants outstanding events resolved, and
			// detaching a suspended debuggee has produced intermittent hangs.
			// In attach mode there is no kill (the client owns the process's
			// lifetime), so restore every patched INT3 first: the debuggee may
			// keep running after we detach, and a leftover trap would crash it.
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
		alive = false;
		emit(EvSessionEnded(requestSeq));
		dbg("disconnect: response emitted");
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
				finishStep();
				stoppedThreadId = outcome.threadId;
				inspector.startStop(outcome.threadId);
				state = Stopped(outcome.threadId);
				currentStoppedBreakpoint = null;
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

		if (userBp == null && !temp) {
			// attach/loader breakpoint or spurious: just keep going
			api.resume(debuggeePid, threadId);
			return;
		}

		// rewind past the INT3 so the trapped instruction can run on the next resume
		api.writeRegister(debuggeePid, threadId, Eip, hitAddress);

		// a real breakpoint always wins over a step landing
		if (userBp != null) {
			finishStep();
			breakpoints.suspend(userBp);
			currentStoppedBreakpoint = userBp;
			stoppedThreadId = threadId;
			inspector.startStop(threadId);
			state = Stopped(threadId);
			emit(EvStoppedBreakpoint(threadId, [userBp.id]));
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
		finishStep();
		currentStoppedBreakpoint = null;
		stoppedThreadId = threadId;
		inspector.startStop(threadId);
		state = Stopped(threadId);
		emit(EvStoppedStep(threadId));
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

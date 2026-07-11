package debug;

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
	var process:DebuggeeProcess;
	var jit:JitInfo;
	var module:ModuleDebugInfo;
	var breakpoints:Breakpoints;
	var handshakeSocket:Socket;
	var stackWalker:StackWalker;
	var stoppedThreadId:Int = 0;
	var currentStoppedBreakpoint:PatchedBreakpoint;
	var alive:Bool = true;

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

	function loop():Void {
		while (alive) {
			switch (state) {
				case Running:
					pollWhileRunning();
				default:
					handleCommand(commands.pop(true));
			}
		}
	}

	function pollWhileRunning():Void {
		var outcome = api.wait(process.pid, WAIT_POLL_MS);
		handleWaitOutcome(outcome);
		// interleave one pending command so setBreakpoints/continue/disconnect are responsive
		var command = commands.pop(false);
		if (command != null) {
			handleCommand(command);
		}
	}

	function handleCommand(command:SessionCommand):Void {
		switch (command) {
			case CmdLaunch(seq, config):
				handleLaunch(seq, config);
			case CmdSetBreakpoints(seq, sourceKey, sourcePath, requested, isReverify):
				handleSetBreakpoints(seq, sourceKey, sourcePath, requested, isReverify);
			case CmdConfigurationDone(seq):
				handleConfigurationDone(seq);
			case CmdContinue(seq, threadId):
				handleContinue(seq, threadId);
			case CmdStackTrace(seq, threadId):
				handleStackTrace(seq, threadId);
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

			var port = DebuggeeProcess.findFreePort();
			process = new DebuggeeProcess(config.hlPath, config.program, config.args, config.cwd, port,
				(category, text) -> emit(EvOutput(category, text)));
			process.startOutputPumps();

			handshakeSocket = connectWithRetries(port);
			// The VM sends the whole handshake then blocks on the socket; drain it
			// fully into memory (a read timeout marks the end) and parse from there.
			// Parsing straight off the socket deadlocks against the two HL processes'
			// send/recv buffering, and byte-at-a-time socket reads are far too slow.
			jit = JitInfoReader.read(new haxe.io.BytesInput(readHandshake()));

			if (!api.start(process.pid)) {
				throw new DebugError("Failed to attach to the debuggee process");
			}
			drainAttachEvents();

			breakpoints = new Breakpoints(api, process.pid);
			stackWalker = new StackWalker(api, process.pid, jit);
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
			var outcome = api.wait(process.pid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case Timeout:
					return;
				case Exit:
					state = Exited;
					emit(EvExited(safeExitCode()));
					return;
				default:
					api.resume(process.pid, outcome.threadId);
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
		api.forceBreak(process.pid);
		for (_ in 0...20) {
			var outcome = api.wait(process.pid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case Timeout: // keep waiting for the forced stop
				case Exit:
					state = Exited;
					emit(EvExited(safeExitCode()));
					return;
				default:
					stoppedThreadId = outcome.threadId;
					return;
			}
		}
	}

	function resumeAfterMemoryWrite():Void {
		api.resume(process.pid, stoppedThreadId);
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
				stepOverAndResume(threadId);
				state = Running;
				emit(EvContinued(requestSeq));
			default:
				emit(EvRejected(requestSeq, "Cannot continue: debuggee is not stopped"));
		}
	}

	// Re-execute the original instruction under the trap flag, re-arm the INT3,
	// then let the debuggee run.
	function stepOverAndResume(threadId:Int):Void {
		var bp = currentStoppedBreakpoint;
		if (bp != null) {
			setTrapFlag(threadId);
			api.resume(process.pid, threadId);
			waitForSingleStep(threadId);
			// clear the trap flag or the debuggee keeps single-stepping forever
			clearTrapFlag(threadId);
			breakpoints.rearm(bp);
			currentStoppedBreakpoint = null;
			if (state == Exited) {
				return; // the debuggee exited during the single step
			}
		}
		api.resume(process.pid, threadId);
	}

	function waitForSingleStep(threadId:Int):Void {
		for (_ in 0...20) {
			var outcome = api.wait(process.pid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case SingleStep:
					return;
				case Exit:
					state = Exited;
					emit(EvExited(safeExitCode()));
					return;
				case Timeout:
				default:
					return;
			}
		}
	}

	function handleStackTrace(requestSeq:Int, threadId:Int):Void {
		switch (state) {
			case Stopped(tid):
				var locations = stackWalker.walk(tid);
				var frames:Array<FrameInfo> = [];
				for (i in 0...locations.length) {
					var location = locations[i];
					var source = module.lookup(location.fidx, location.op);
					frames.push({
						id: i,
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

	function handleDisconnect(requestSeq:Int):Void {
		if (process != null) {
			try {
				api.stop(process.pid);
			} catch (e:Dynamic) {}
			process.kill();
			process.close();
		}
		closeHandshake();
		alive = false;
		emit(EvSessionEnded(requestSeq));
	}

	// --- wait-event classification while running ---

	function handleWaitOutcome(outcome:WaitOutcome):Void {
		switch (outcome.result) {
			case Timeout:
				// nothing pending
			case Exit:
				state = Exited;
				emit(EvExited(safeExitCode()));
			case Breakpoint:
				handleBreakpointHit(outcome.threadId);
			case SingleStep:
				api.resume(process.pid, outcome.threadId);
			case Error, StackOverflow:
				stoppedThreadId = outcome.threadId;
				state = Stopped(outcome.threadId);
				currentStoppedBreakpoint = null;
				emit(EvStoppedException(outcome.threadId, outcome.result == StackOverflow ? "Stack overflow" : "Unhandled exception"));
			case Handled:
				api.resume(process.pid, outcome.threadId);
		}
	}

	function handleBreakpointHit(threadId:Int):Void {
		// INT3 leaves the instruction pointer one byte past the trap
		var eip = api.readRegister(process.pid, threadId, Eip);
		var breakpointAddress = Int64.sub(eip, Int64.ofInt(1));
		var bp = breakpoints != null ? breakpoints.atAddress(breakpointAddress) : null;
		if (bp == null) {
			// attach/loader breakpoint or spurious: just keep going
			api.resume(process.pid, threadId);
			return;
		}
		// rewind past the INT3 and restore the original byte so the instruction can run on continue
		api.writeRegister(process.pid, threadId, Eip, breakpointAddress);
		breakpoints.suspend(bp);
		currentStoppedBreakpoint = bp;
		stoppedThreadId = threadId;
		state = Stopped(threadId);
		emit(EvStoppedBreakpoint(threadId, [bp.id]));
	}

	// --- helpers ---

	function setTrapFlag(threadId:Int):Void {
		var flags = Int64.toInt(api.readRegister(process.pid, threadId, EFlags));
		api.writeRegister(process.pid, threadId, EFlags, Int64.ofInt(flags | TRAP_FLAG));
	}

	function clearTrapFlag(threadId:Int):Void {
		var flags = Int64.toInt(api.readRegister(process.pid, threadId, EFlags));
		api.writeRegister(process.pid, threadId, EFlags, Int64.ofInt(flags & ~TRAP_FLAG));
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
		if (process != null) {
			process.kill();
			process.close();
			process = null;
		}
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

package ijhaxe.debug.session;
import haxe.io.Bytes;
import haxe.io.FPHelper;
import ijhaxe.debug.DebugErrorCode;
import ijhaxe.debug.HostPlatform;
import ijhaxe.debug.Trace;
import ijhaxe.dap.protocol.Breakpoint;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.breakpoints.Breakpoints;
import ijhaxe.debug.breakpoints.PatchedBreakpoint;
import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.module.ExceptionSites;
import ijhaxe.debug.module.NativeThrowResolver;
import ijhaxe.debug.module.TryRegions;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.JitInfoReader;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.DebugApi;
import ijhaxe.debug.target.DebuggeeProcess;
import ijhaxe.debug.target.MemoryReader;
import ijhaxe.debug.target.MemoryWriter;
import ijhaxe.debug.target.StackFrameLocation;
import ijhaxe.debug.target.StackWalker;
import ijhaxe.debug.target.ThreadInfo;
import ijhaxe.debug.target.ThreadRegistry;
import ijhaxe.debug.target.VmExceptionControl;
import ijhaxe.debug.target.WaitOutcome;
import ijhaxe.debug.inspect.CpuRegisters;
import ijhaxe.debug.inspect.VariableInspector;

import haxe.Int64;
import sys.net.Host;
import sys.net.Socket;
import sys.thread.Deque;
import sys.thread.Thread;

/**
	Owns the whole debug session on a single dedicated thread — the only thread
	that touches DebugApi (required on Windows, where WaitForDebugEvent must run
	on the attaching thread). Commands come in on a Deque; results and stop/exit
	events go out through the `emit` callback.

	The session keeps the lifecycle (launch/attach/disconnect), the command
	loop, the wait-outcome routing and the shared trap machinery (trap dance,
	enterStopped, force break); the feature logic lives in friend controllers —
	SteppingController, LineBreakpointController, ExceptionController — plus
	EvalCallInjector, which access the session's state through
	@:access(ijhaxe.debug.session.DebugSession).
**/
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
	// Truncation GUARD on the handshake socket, not a delimiter: the handshake
	// is parsed to its self-described end, so on a healthy launch no read ever
	// waits this long. It only fires when the VM dies/stalls mid-handshake,
	// turning a would-be forever-hang into a clean launch failure.
	static inline var HANDSHAKE_READ_TIMEOUT_S = 3.0;
	// spawn attempts when the VM loses its debug port to another socket
	// (the findFreePort reservation race) - see spawnAndHandshake
	static inline var LAUNCH_BIND_ATTEMPTS = 3;

	final api:DebugApi;
	final commands = new Deque<SessionCommand>();
	final emit:DebugEvent->Void;

	var state:SessionState = NotStarted;
	// In launch mode the adapter spawns and owns the debuggee (`process` set);
	// in attach mode the client spawned it and `process` stays null. All debug
	// natives key on the pid, so everything downstream uses `debuggeePid`.
	var process:DebuggeeProcess;
	var debuggeePid:Int = 0;
	// linux only: the attach SIGSTOP is HELD (not resumed) until
	// configurationDone, so launch-time breakpoint installs hit a
	// ptrace-stopped tracee; -1 = nothing held. See drainAttachEvents.
	var heldAttachStop:Int = -1;
	var jit:JitInfo;
	var module:ModuleDebugInfo;
	var breakpoints:Breakpoints;
	// Throw-site enumerator + static try-region analysis + hl_throw entry
	// control (all built once jit+module are ready); the exception FILTER
	// state and hit handling live in the ExceptionController.
	var exceptionSites:ExceptionSites;
	var tryRegions:TryRegions;
	var nativeThrowResolver:NativeThrowResolver;
	var vmExceptions:Null<VmExceptionControl> = null;
	var memReader:MemoryReader;
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
	// exception-stop text assembly and throw classification (created at launch)
	var descriptions:StopDescriptions;
	var throwClassifier:ThrowClassifier;
	// the feature controllers (friends via @:access): stepping, source line
	// breakpoints and exception breakpoints; the session routes commands and
	// trap hits to them and provides the shared trap machinery
	final stepping:SteppingController;
	final lineBreakpoints:LineBreakpointController;
	final exceptions:ExceptionController;

	public function new(api:DebugApi, emit:DebugEvent->Void) {
		this.api = api;
		this.emit = emit;
		stepping = new SteppingController(this);
		lineBreakpoints = new LineBreakpointController(this);
		exceptions = new ExceptionController(this);
	}

	/**
		Starts the session thread.
	**/
	public function start():Void {
		Thread.create(loop);
	}

	/**
		Queues a command for the session thread.
	**/
	public function send(command:SessionCommand):Void {
		commands.add(command);
		if (debuggeePid != 0 && state == Running && HostPlatform.isPtraceBased()) {
			// linux: while Running the session thread is parked in the BLOCKING
			// debug wait (hl's linux debug_wait ignores its timeout), so a queued
			// command would sit until some debug event happens to arrive —
			// observable as pause requests timing out. Poke the debuggee
			// (force-break = kill SIGTRAP on the traced main thread): the wait
			// returns, the trap matches no known patch site and is resumed silently,
			// and pollWhileRunning's command interleave runs this command. The
			// races are benign — a missed nudge just waits for the next event, a
			// spurious one is a silently-resumed stop.
			api.forceBreak(debuggeePid);
		}
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
			reject(seqOf(command), 'Internal debugger error: ${Std.string(e)}');
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
			case CmdStep(seq, _, _, _): seq;
			case CmdStepInTargets(seq, _): seq;
			case CmdPause(seq, _): seq;
			case CmdSetExceptionBreakpoints(seq, _, _): seq;
			case CmdSetToStringRendering(seq, _): seq;
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
		// runs for every request: don't pay the concat + reflection when tracing is off
		if (Trace.isEnabled()) {
			dbg("cmd " + Type.enumConstructor(command));
		}
		switch (command) {
			case CmdLaunch(seq, config):
				handleLaunch(seq, config);
			case CmdSetBreakpoints(seq, sourceKey, sourcePath, requested, isReverify):
				lineBreakpoints.handleSetBreakpoints(seq, sourceKey, sourcePath, requested, isReverify);
			case CmdConfigurationDone(seq):
				handleConfigurationDone(seq);
			case CmdContinue(seq, threadId):
				handleContinue(seq, threadId);
			case CmdStep(seq, threadId, mode, targetId):
				stepping.handleStep(seq, threadId, mode, targetId);
			case CmdStepInTargets(seq, frameId):
				stepping.handleStepInTargets(seq, frameId);
			case CmdPause(seq, threadId):
				handlePause(seq, threadId);
			case CmdSetExceptionBreakpoints(seq, filters, filterTypes):
				exceptions.setFilters(seq, filters, filterTypes);
			case CmdSetToStringRendering(seq, enabled):
				// stored for the fault-proof (hl_dyn_call_safe) rendering to come;
				// labels stay class names until an injected toString CANNOT fault
				// (a faulted injected call is unrecoverable — the debug API cannot
				// continue past it). Accepting and remembering the flag now keeps
				// the wire contract stable for the IDE's live toggle.
				if (inspector != null) {
					inspector.renderWithToString = enabled;
				}
				emit(EvToStringRenderingSet(seq));
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
				jit = readHandshake();
			} else {
				jit = spawnAndHandshake(config);
			}
			// register reads/writes must use the DEBUGGEE's context layout: with the
			// wrong bitness a 32-bit debuggee's registers read as garbage and EIP
			// writes corrupt the thread (crash on the first continue past an INT3)
			api.setTargetIs64(jit.is64);

			if (!api.start(debuggeePid)) {
				throw new DebugError("Failed to attach to the debuggee process");
			}
			drainAttachEvents();

			initializeSessionServices();
			exceptions.apply(); // honour a pre-launch setExceptionBreakpoints
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

	/**
		Launch mode: spawns the debuggee on a reserved port and reads the debug
		handshake. The reservation (findFreePort) must be RELEASED before the VM
		can bind it, and in that window another socket can take the port: the VM
		prints "Could not start debugger on port N" and whatever owns the port
		drops the connection, surfacing as a connect failure or a handshake EOF.
		That case retries the whole spawn on a fresh port instead of failing the
		launch.
	**/
	function spawnAndHandshake(config:LaunchConfig):JitInfo {
		for (attempt in 1...LAUNCH_BIND_ATTEMPTS + 1) {
			var port = DebuggeeProcess.findFreePort();
			process = new DebuggeeProcess(config.hlPath, config.program, config.args, config.cwd, port,
				(category, text) -> emit(EvOutput(category, text)));
			process.startOutputPumps();
			debuggeePid = process.pid;
			try {
				handshakeSocket = connectWithRetries(port);
				return readHandshake();
			} catch (e:DebugError) {
				if (!lostPortBind()) {
					throw e;
				}
				if (attempt == LAUNCH_BIND_ATTEMPTS) {
					throw new DebugError('The VM could not bind its debug port (taken by another process, $LAUNCH_BIND_ATTEMPTS attempts)');
				}
				emit(EvOutput("stderr", 'debug port $port was taken before the VM could bind it - relaunching\n'));
				discardFailedSpawn();
			}
		}
		throw new DebugError("unreachable");
	}

	// The VM prints the bind failure at startup, but the stderr pump may not
	// have delivered it yet when the connect or handshake fails - give it a
	// beat before deciding.
	function lostPortBind():Bool {
		if (process == null) {
			return false;
		}
		if (!process.debugBindFailed) {
			Sys.sleep(0.1);
		}
		return process.debugBindFailed;
	}

	function discardFailedSpawn():Void {
		closeHandshake();
		process.kill();
		process.close();
		process = null;
		debuggeePid = 0;
	}

	/**
		The VM sends the whole handshake then blocks on the socket. The format
		is self-delimiting (JitInfoReader derives every size as it parses and
		never over-reads), so parse straight off a buffered view: it recvs in
		large chunks but only refills when the parser still NEEDS bytes, so it
		cannot block after the final handshake byte. The socket timeout is a
		truncation guard only - on a healthy handshake it never fires. (The
		previous approach slurped until a 0.5s read timeout marked the end,
		a dead half-second on EVERY launch.)
	**/
	function readHandshake():JitInfo {
		handshakeSocket.setTimeout(HANDSHAKE_READ_TIMEOUT_S);
		try {
			return JitInfoReader.read(new HandshakeInput(handshakeSocket.input));
		} catch (e:DebugError) {
			throw e;
		} catch (e:haxe.io.Eof) {
			throw new DebugError("The debuggee closed the connection mid-handshake");
		} catch (e:Dynamic) {
			throw new DebugError('Debug handshake stalled or unreadable: ${Std.string(e)}');
		}
	}

	// Builds every attached-session collaborator (breakpoints, walkers, readers,
	// the inspector) and wires the inspector's session callbacks. Requires
	// `module`, `jit`, `debuggeePid` and a started debug attach.
	function initializeSessionServices():Void {
		breakpoints = new Breakpoints(api, debuggeePid);
		exceptionSites = new ExceptionSites(module, jit);
		tryRegions = new TryRegions(module);
		stackWalker = new StackWalker(api, debuggeePid, jit);
		memReader = new MemoryReader(api, debuggeePid, jit.is64);
		nativeThrowResolver = new NativeThrowResolver(module, jit, memReader, exceptionSites);
		// one arch descriptor resolved from the handshake, shared by every
		// raw-memory reader here (the inspector builds its own from the same jit)
		var align = new Align(jit.is64, jit.boolSize4);
		threadRegistry = new ThreadRegistry(memReader, align, jit.hlVersionMajor, jit.hlVersionMinor);
		vmExceptions = new VmExceptionControl(api, debuggeePid, memReader, align, jit.threadsPtr);
		// linux: lets the walker recover the interrupted JIT frame through the
		// kernel signal frame when a VM error arrived via SIGSEGV (null access)
		stackWalker.capturedStack = vmExceptions.capturedStack;
		inspector = new VariableInspector(module, jit, memReader);
		descriptions = new StopDescriptions(module, inspector);
		throwClassifier = new ThrowClassifier(api, debuggeePid, jit, memReader, stackWalker, tryRegions, inspector);
		// Frames parked at hl_throw's ENTRY serve the stop reported at hl_throw's
		// own break: by then execution is deep inside hl_throw, where the frame
		// chain is no longer walkable. Consumed on first use; framesFor caches it
		// for the rest of the stop.
		inspector.frameWalker = tid -> {
			var parked = exceptions.consumeParkedFrames(tid);
			// an EMPTY parked walk (a signal-delivered VM error on linux: the
			// entry-time chain is unwalkable AND the VM's exc capture does not
			// exist yet) must not shadow a live walk — at the throw break the
			// capture is populated and the signal-frame recovery can use it
			return parked != null && parked.length > 0 ? parked : stackWalker.walk(tid);
		};
		var cpuRegisters = new CpuRegisters(api, debuggeePid);
		// CPU registers are only readable while stopped; the callback is invoked
		// for a stopped thread's top frame, but guard defensively.
		inspector.cpuRegistersFor = tid -> state.match(Stopped(_)) ? cpuRegisters.rows(tid) : [];
		inspector.enableWrites(new MemoryWriter(api, debuggeePid, jit.is64));
		// eval-calls run in the debuggee via the injector; its hooks are the only
		// session state an injected call touches
		var evalCalls = new EvalCallInjector(api, debuggeePid, jit, breakpoints, {
			keepSuspended: () -> currentStoppedBreakpoint != null ? currentStoppedBreakpoint.address : null,
			onForeignStop: outcome -> pendingForeignStop = outcome,
			onExited: threadId -> {
				state = Exited;
				releaseExitedProcess(threadId);
			}
		});
		inspector.xmm0Writer = value -> {
			var bits = FPHelper.doubleToI64(value);
			if (jit.is64 && HostPlatform.isPtraceBased()) {
				// linux: hl's debug_write_register cannot write XMM (its ptrace
				// write path never handled the FP pseudo-offsets the read path
				// defines - the write silently fails), so load the register by
				// running an injected stub, eval-call style
				evalCalls.writeXmm0(stoppedThreadId, bits);
			} else {
				api.writeRegister(debuggeePid, stoppedThreadId, Xmm0, bits);
			}
		};
		inspector.warnSink = text -> emit(EvOutput("console", text));
		inspector.functionCaller = (funcAddr, args, floatBits) ->
			evalCalls.call(stoppedThreadId, funcAddr, args, floatBits);
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
		throw new DebugError('Could not connect to the debuggee debug port: ${Std.string(lastError)}');
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
					emitExitedAfterOutputDrain();
					return;
				default:
					if (HostPlatform.isPtraceBased()) {
						// linux delivers exactly ONE attach stop (the PTRACE_ATTACH
						// SIGSTOP) and hl's linux debug_wait IGNORES its timeout —
						// it is a plain blocking waitpid, so a second drain wait
						// here deadlocks the session (kernel do_wait forever).
						// Moreover linux ptrace memory writes only work on a
						// ptrace-STOPPED tracee (Windows' WriteProcessMemory works
						// on a running process), so the launch-time breakpoint
						// installs need the debuggee held. Keep the attach stop —
						// the debuggee is gated on the handshake socket anyway —
						// and release it when configurationDone opens that gate.
						heldAttachStop = outcome.threadId;
						return;
					}
					api.resume(debuggeePid, outcome.threadId);
			}
		}
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
					emitExitedAfterOutputDrain();
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
		// linux: the process itself is still in the held attach stop; release
		// it now that the breakpoints are installed (see drainAttachEvents)
		if (heldAttachStop != -1) {
			api.resume(debuggeePid, heldAttachStop);
			heldAttachStop = -1;
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
					emitExitedAfterOutputDrain();
					return null;
				case Breakpoint, Error, StackOverflow:
					return outcome;
			}
		}
		dbg("waitForSingleStep: no single-step event after 100 polls");
		return null;
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
					reject(requestSeq, 'Cannot evaluate: ${Std.string(e)}');
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
					reject(requestSeq, 'Cannot set value: ${Std.string(e)}');
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
				emitExitedAfterOutputDrain();
			case Breakpoint:
				handleBreakpointHit(outcome.threadId);
			case SingleStep:
				api.resume(debuggeePid, outcome.threadId);
			case Error, StackOverflow:
				enterStopped(outcome.threadId, null);
				emit(EvStoppedException(outcome.threadId, descriptions.runtimeError(outcome.threadId, outcome.result == StackOverflow)));
			case Handled:
				// hl_debug_wait already continued this event internally (thread
				// create/exit/set-name, dll load, ...). Continuing again is at
				// best a silent failure and at worst blindly continues a REAL
				// event that arrived in the meantime — do nothing.
				if (outcome.threadId == -1 && HostPlatform.isPtraceBased()) {
					// EXCEPT with tid -1: linux waitpid() FAILED (ECHILD) — the
					// debuggee died without a parseable exit status:
					// an INT3 executed by an UNTRACED worker thread kills the whole
					// process (SIGTRAP default action; linux traces per-thread and
					// only the main thread is attached), debug.c maps the
					// WIFSIGNALED status to this same code, and every later wait
					// returns -1 — an infinite spin unless it is treated as death.
					// (Windows WaitForDebugEvent events always carry a real tid,
					// but the platform gate keeps this branch provably inert there.)
					finishStep();
					state = Exited;
					releaseExitedProcess(outcome.threadId);
					emitExitedAfterOutputDrain();
				}
		}
	}

	// Classifies an INT3 stop by what is patched at the trap address and routes
	// it: a user breakpoint always wins, then an armed exception site, then the
	// hl_throw entry trap, then a step temp; a trap with no known patch site
	// is an unpatched trap (hl_throw's own break, or attach/loader noise).
	function handleBreakpointHit(threadId:Int):Void {
		// INT3 leaves the instruction pointer one byte past the trap
		var eip = api.readRegister(debuggeePid, threadId, Eip);
		var hitAddress = Int64.sub(eip, Int64.ofInt(1));
		var userBp = breakpoints != null ? breakpoints.atAddress(hitAddress) : null;
		var temp = breakpoints != null && breakpoints.isTemp(hitAddress);
		var excEntry = breakpoints != null ? breakpoints.exceptionAt(hitAddress) : null;
		var nativeThrow = breakpoints != null && breakpoints.isNativeThrow(hitAddress);

		if (userBp == null && !temp && excEntry == null && !nativeThrow) {
			handleUnpatchedTrap(threadId);
			return;
		}

		// rewind past the INT3 so the trapped instruction can run on the next resume
		api.writeRegister(debuggeePid, threadId, Eip, hitAddress);

		if (userBp != null) {
			lineBreakpoints.handleHit(threadId, userBp);
		} else if (excEntry != null) {
			exceptions.handleSiteHit(threadId, excEntry);
		} else if (nativeThrow) {
			exceptions.handleNativeThrowHit(threadId);
		} else {
			stepping.handleTempHit(threadId, hitAddress);
		}
	}

	// A trap with no known patch site: either hl_throw's own
	// hl_debug_break completing a parked VM throw (the ExceptionController
	// reports that stop), or an attach/loader breakpoint / spurious trap,
	// resumed past silently.
	function handleUnpatchedTrap(threadId:Int):Void {
		if (exceptions.handleVmThrowBreak(threadId)) {
			return;
		}
		api.resume(debuggeePid, threadId);
	}

	// Resume past a trap that must NOT stop the debuggee (a false breakpoint
	// condition, a filtered exception, hl_throw's silent passthrough), and
	// process any stop another thread raised while we were doing so.
	function resumePastSuppressedTrap(threadId:Int, bp:PatchedBreakpoint):Void {
		inspector.invalidate();
		var interrupted = resumePastUserBreakpoint(threadId, bp);
		if (state == Exited) {
			return;
		}
		if (interrupted != null) {
			handleWaitOutcome(interrupted);
		}
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

	/**
		Drains the debuggee's remaining output BEFORE the exited event: a dead
		process's pipes still hold their buffered tail, and an output event
		trailing `exited` is lost on clients that stop listening at exit —
		observable under machine load as a run's final stdout lines missing.
		The pipes EOF promptly once the process is dead; the timeout is a guard.
		Attach mode has no pumps (`process` is null).
	**/
	function emitExitedAfterOutputDrain():Void {
		if (process != null) {
			process.awaitOutputDrained(1.0);
		}
		emit(EvExited(safeExitCode()));
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

/**
	Buffered view over the handshake socket for JitInfoReader: recvs in large
	chunks (a byte-at-a-time socket parse would be tens of thousands of
	syscalls) but only refills when the parser still NEEDS bytes — so it can
	never block waiting for data past the final handshake byte. Safe because
	the handshake format is self-delimiting and JitInfoReader never over-reads;
	a refill therefore only happens for bytes the VM has sent or is sending.
**/
private class HandshakeInput extends haxe.io.Input {
	static inline var CHUNK = 65536;

	final source:haxe.io.Input;
	final buffer:Bytes;
	var position = 0;
	var available = 0;

	public function new(source:haxe.io.Input) {
		this.source = source;
		this.buffer = Bytes.alloc(CHUNK);
	}

	override public function readByte():Int {
		if (available == 0) {
			refill();
		}
		var value = buffer.get(position);
		position++;
		available--;
		return value;
	}

	override public function readBytes(target:Bytes, offset:Int, length:Int):Int {
		if (available == 0) {
			refill();
		}
		var count = length < available ? length : available;
		target.blit(offset, buffer, position, count);
		position += count;
		available -= count;
		return count;
	}

	function refill():Void {
		// blocks until SOME data arrives (bounded by the socket's guard
		// timeout); returns a partial chunk rather than waiting for CHUNK bytes
		available = source.readBytes(buffer, 0, CHUNK);
		position = 0;
		if (available <= 0) {
			throw new haxe.io.Eof();
		}
	}
}

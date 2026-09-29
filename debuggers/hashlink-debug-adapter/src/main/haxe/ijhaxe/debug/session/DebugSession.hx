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
	Runs the whole debug session on one dedicated thread, the only thread that
	touches DebugApi. Windows requires this: WaitForDebugEvent must run on the
	thread that attached. Commands arrive on a Deque; results, stops and exits
	leave through the `emit` callback.

	The session owns the lifecycle (launch, attach, disconnect), the command
	loop, the routing of wait outcomes and the shared trap machinery (the trap
	dance, enterStopped, forced breaks). The feature logic lives in three
	controllers: SteppingController, LineBreakpointController and
	ExceptionController. They are friends of the session: they read and write
	its state through @:access(ijhaxe.debug.session.DebugSession).
	EvalCallInjector reaches the session only through its EvalCallHooks.
**/
class DebugSession {

	// After attaching, Windows delivers a burst of startup debug events (the
	// initial breakpoint, DLL loads, thread creation). drainAttachEvents resumes
	// past each until a wait times out. This caps how many it clears, so an
	// endless stream of events cannot loop forever.
	static inline var MAX_ATTACH_DRAIN_EVENTS = 20;
	// A forced break (a user pause) arrives asynchronously, possibly behind a few
	// auto-continued events. forceBreakAndDrain polls for it and gives up after
	// MAX_FORCE_BREAK_POLLS * ATTACH_DRAIN_MS (about 1s).
	static inline var MAX_FORCE_BREAK_POLLS = 20;

	static inline var TRAP_FLAG = 0x100;
	static inline var WAIT_POLL_MS = 20;
	static inline var ATTACH_DRAIN_MS = 50;
	static inline var CONNECT_RETRIES = 60;
	static inline var CONNECT_DELAY_MS = 50;
	// Guards the handshake socket against a truncated handshake; it does not mark
	// the end. The parser stops at the handshake's self-described end, so a
	// healthy launch never waits this long. The timeout fires only when the VM
	// dies or stalls mid-handshake, and turns that hang into a launch failure.
	static inline var HANDSHAKE_READ_TIMEOUT_S = 3.0;
	// spawn attempts when another socket takes the debug port before the VM
	// binds it (see spawnAndHandshake)
	static inline var LAUNCH_BIND_ATTEMPTS = 3;

	final api:DebugApi;
	final commands = new Deque<SessionCommand>();
	final emit:DebugEvent->Void;

	var state:SessionState = NotStarted;

	// In launch mode the adapter spawns and owns the debuggee, and `process` is
	// set. In attach mode the client spawned it and `process` stays null. All
	// debug natives take the pid, so everything downstream uses `debuggeePid`.
	var process:DebuggeeProcess;
	var debuggeePid:Int = 0;

	// Linux only: the thread of the attach SIGSTOP, held until
	// configurationDone so that launch-time breakpoint installs hit a
	// ptrace-stopped tracee. -1 when nothing is held. See drainAttachEvents.
	var heldAttachStop:Int = -1;

	var jit:JitInfo;
	var module:ModuleDebugInfo;
	var breakpoints:Breakpoints;

	// The throw sites, the static try-region analysis and the hl_throw entry
	// control, built once jit and module are ready. The exception filter state
	// and the hit handling live in ExceptionController.
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

	// A user pause holds the debug event of the thread the forced break landed
	// on. On Windows that is a transient system thread, not an HL thread. The
	// process unfreezes only when that exact thread is continued, so it is kept
	// here for the next resume while an HL thread is reported for inspection.
	// -1 when no pause event is held.
	var pauseEventThread:Int = -1;
	// True from a forceBreak until its stop arrives. forceBreakAndDrain
	// usually takes that stop; when resuming past a racing breakpoint hit
	// swallows it as a stray trap instead, the drain issues the break again.
	var forcedBreakPending:Bool = false;

	var alive:Bool = true;

	// The in-flight step (temps planted, landing pending), bound to its thread;
	// null when no step is active. See ActiveStep for why one field suffices.
	var activeStep:Null<ActiveStep> = null;

	// A pending debug event from another thread that interrupted an eval-call.
	// It owns the process freeze and is processed as a normal stop once the
	// current command finishes. Processing it mid-eval would re-enter the
	// inspector while its caches are in use.
	var pendingForeignStop:Null<WaitOutcome> = null;

	// Variable inspection, created at launch once jit and module exist. Owns the
	// per-stop frame cache and the variablesReference registry.
	var inspector:VariableInspector;

	// exception-stop texts and throw classification, created at launch
	var descriptions:StopDescriptions;
	var throwClassifier:ThrowClassifier;

	// the feature controllers; the session routes commands and trap hits to them
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
			// Linux: while Running, the session thread is parked in the debug wait,
			// which blocks because hl's linux debug_wait ignores its timeout. A
			// queued command would wait for the next debug event, and a pause
			// request would time out. A forced break (SIGTRAP on the traced main
			// thread) ends the wait: the trap matches no patch site and is resumed
			// silently, and pollWhileRunning then runs the command. The races are
			// harmless. A missed nudge waits for the next event, and an extra one
			// is a silently resumed stop.
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
		// run one pending command per poll, so setBreakpoints, pause and disconnect stay responsive
		var command = commands.pop(false);
		if (command != null) {
			handleCommand(command);
		}
	}

	// A handler bug must never kill the session thread. Without it, every later
	// request times out and the client's views stay blank for good. A failure
	// therefore rejects only its own command, and the loop carries on.
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
		// Another thread's stop may have interrupted an eval-call. That event owns
		// the process freeze and is processed only now that the command has
		// settled, never mid-eval while the inspector's caches are in use.
		var foreign = pendingForeignStop;
		if (foreign != null) {
			pendingForeignStop = null;
			handleWaitOutcome(foreign);
		}
	}

	// Rejects a request with a DebugError's machine-readable code and variables,
	// which become the DAP Message.id and Message.variables the client branches on.
	inline function rejectError(requestSeq:Int, e:DebugError):Void {
		emit(EvRejected(requestSeq, e.message, e.code, e.variables));
	}

	// Rejects a request with a plain message and the generic error code.
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
		// this runs for every request: build the trace text only when tracing
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
				// Stored only. Labels stay class names, because a faulting injected
				// toString cannot be recovered: the debug API cannot continue past
				// the fault. Accepting the flag keeps the wire contract stable for
				// the IDE's live toggle.
				// TODO: render toString labels once the call is fault-proof (hl_dyn_call_safe).
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
				// and owns the process's stdio and lifetime. A spawn from here would
				// go through HL's process.c, which hides the debuggee's first window
				// on Windows (see docs/README.md).
				debuggeePid = config.attachPid;
				handshakeSocket = connectWithRetries(config.debugPort);
				jit = readHandshake();
			} else {
				jit = spawnAndHandshake(config);
			}
			// Register access must use the debuggee's context layout. With the wrong
			// bitness, a 32-bit debuggee's registers read as garbage and EIP writes
			// corrupt the thread, which crashes on the first continue past an INT3.
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
		handshake. findFreePort must release its reservation before the VM can
		bind the port, and another socket can take the port in between. The VM
		then prints "Could not start debugger on port N", and the connect or the
		handshake fails. In that case the whole spawn is retried on a fresh port.
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
				if (!debugPortWasTaken()) {
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
	// have delivered it yet when the connect or handshake fails, so this waits
	// briefly before deciding.
	function debugPortWasTaken():Bool {
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
		Parses the handshake, which the VM sends in full before it blocks on the
		socket. The format is self-delimiting: JitInfoReader derives every size
		as it parses and never reads past the end. HandshakeInput therefore reads
		in large chunks but refills only when the parser needs more bytes, so it
		never blocks after the final handshake byte. The socket timeout only
		guards against a truncated handshake.
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

	// Builds the services of an attached session (breakpoints, stack walker,
	// memory readers, the inspector) and wires the inspector's callbacks into the
	// session. Requires `module`, `jit`, `debuggeePid` and a completed attach.
	function initializeSessionServices():Void {
		breakpoints = new Breakpoints(api, debuggeePid);
		exceptionSites = new ExceptionSites(module, jit);
		tryRegions = new TryRegions(module);
		stackWalker = new StackWalker(api, debuggeePid, jit);
		memReader = new MemoryReader(api, debuggeePid, jit.is64);
		nativeThrowResolver = new NativeThrowResolver(module, jit, memReader, exceptionSites);

		// one architecture descriptor from the handshake, shared by the raw-memory
		// readers built here (the inspector builds its own from the same jit)
		var align = new Align(jit.is64, jit.boolSize4);
		threadRegistry = new ThreadRegistry(memReader, align, jit.hlVersionMajor, jit.hlVersionMinor);
		vmExceptions = new VmExceptionControl(api, debuggeePid, memReader, align, jit.threadsPtr);

		// Linux: lets the walker recover the interrupted JIT frame from the VM's
		// own throw-time stack capture when a VM error arrived as a signal (a null
		// access raises SIGSEGV).
		stackWalker.capturedStack = vmExceptions.capturedStack;

		inspector = new VariableInspector(module, jit, memReader);
		descriptions = new StopDescriptions(module, inspector);
		throwClassifier = new ThrowClassifier(api, debuggeePid, jit, memReader, stackWalker, tryRegions, inspector);

		// Frames walked at hl_throw's entry serve the stop reported at hl_throw's
		// own break, deep inside hl_throw, where the frame chain can no longer be
		// walked. They are consumed on first use; framesFor caches them for the
		// rest of the stop.
		inspector.frameWalker = tid -> {
			var parked = exceptions.consumeParkedFrames(tid);
			// An empty parked walk must not hide a live walk. It happens on linux
			// for a VM error delivered as a signal: at the entry the chain cannot
			// be walked and the VM's stack capture does not exist yet. At the
			// throw break the capture is filled in and the walker can recover
			// from it.
			return parked != null && parked.length > 0 ? parked : stackWalker.walk(tid);
		};
		var cpuRegisters = new CpuRegisters(api, debuggeePid);
		// CPU registers are readable only while stopped. The inspector asks only
		// for a stopped thread's top frame, but the state is checked anyway.
		inspector.cpuRegistersFor = tid -> state.match(Stopped(_)) ? cpuRegisters.rows(tid) : [];
		inspector.enableWrites(new MemoryWriter(api, debuggeePid, jit.is64));
		// eval-calls run in the debuggee through the injector; its hooks are the
		// only session state an injected call touches
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
				// Linux: hl's debug_write_register cannot write XMM registers. Its
				// ptrace write path lacks the FP offsets the read path defines, and
				// the write fails silently. An injected stub loads the register
				// instead, the same way eval-calls run.
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

	// Consumes the events the OS raises at attach time (the Windows attach
	// breakpoint, DLL loads), so the debuggee is back in a clean state.
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
						// Linux delivers exactly one attach stop (the PTRACE_ATTACH
						// SIGSTOP), and hl's linux debug_wait ignores its timeout: it
						// is a plain blocking waitpid, so a second drain wait would
						// deadlock the session. Linux ptrace memory writes also need
						// a ptrace-stopped tracee (Windows' WriteProcessMemory works
						// on a running process), and the launch-time breakpoint
						// installs are such writes. So the attach stop is held;
						// the handshake socket holds the debuggee anyway.
						// configurationDone releases both.
						heldAttachStop = outcome.threadId;
						return;
					}
					api.resume(debuggeePid, outcome.threadId);
			}
		}
	}

	// Stops the running debuggee so its memory can be patched; the stop is
	// never shown to the user. Returns true when resumeAfterMemoryWrite must
	// let the debuggee run again afterwards. Returns false when no forced stop
	// arrived, or when the debuggee stopped by itself (a breakpoint, a runtime
	// error) before the interrupt landed: that stop has been reported to the
	// user and must not be resumed behind their back.
	function pauseForMemoryWrite():Bool {
		var outcome = forceBreakAndDrain();
		if (outcome == null) {
			return false;
		}
		stoppedThreadId = outcome.threadId;
		return true;
	}

	// Interrupts the running debuggee (forceBreak) and drains events until the
	// forced stop arrives: a trap at an address the session did not patch
	// (Windows raises it on a thread of its own, linux sends a SIGTRAP),
	// possibly after a few auto-continued lifecycle events.
	// The debuggee may stop by itself before the interrupt lands (a patched
	// trap, a runtime error). That stop is handled like any other and owns the
	// frozen debuggee; the forced stop then arrives as a stray trap on a later
	// resume.
	// Returns the forced stop, or null when the debuggee exited (EvExited was
	// emitted), stopped by itself (state is Stopped), or did not stop within
	// MAX_FORCE_BREAK_POLLS. Serves both the memory-write pause and the user's
	// pause.
	function forceBreakAndDrain():Null<WaitOutcome> {
		var forced = drainForForcedStop();
		forcedBreakPending = false;
		return forced;
	}

	function drainForForcedStop():Null<WaitOutcome> {
		issueForcedBreak();
		for (_ in 0...MAX_FORCE_BREAK_POLLS) {
			var outcome = api.wait(debuggeePid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case Timeout: // keep waiting for the forced stop
				case Handled:
					// an auto-continued lifecycle event that froze no thread: not the
					// forced stop, keep waiting
				case Breakpoint if (!isPatchedTrap(outcome.threadId)):
					return outcome;
				default:
					handleWaitOutcome(outcome);
					if (state != Running) {
						return null;
					}
					// resuming past the racing stop may have consumed the forced
					// stop as a stray trap; the debuggee runs again, so break anew
					if (!forcedBreakPending) {
						issueForcedBreak();
					}
			}
		}
		return null;
	}

	function issueForcedBreak():Void {
		forcedBreakPending = true;
		api.forceBreak(debuggeePid);
	}

	// Whether the thread's trap is one of the session's own INT3s rather than
	// a foreign one (a forced break, the attach or loader breakpoint).
	function isPatchedTrap(threadId:Int):Bool {
		if (breakpoints == null) {
			return false;
		}
		// INT3 leaves the instruction pointer one byte past the trap
		var eip = api.readRegister(debuggeePid, threadId, Eip);
		return breakpoints.isPatchedSite(Int64.sub(eip, Int64.ofInt(1)));
	}

	function resumeAfterMemoryWrite():Void {
		api.resume(debuggeePid, stoppedThreadId);
	}

	// --- run control ---

	function handleConfigurationDone(requestSeq:Int):Void {
		emit(EvConfigurationDone(requestSeq));
		// closing the handshake socket releases the debuggee: the VM's 1-byte
		// recv returns and user code starts
		if (handshakeSocket != null) {
			try {
				handshakeSocket.close();
			} catch (e:Dynamic) {}
			handshakeSocket = null;
		}
		// Linux: the process is still in the held attach stop; release it now
		// that the breakpoints are installed (see drainAttachEvents).
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
					// another thread stopped the debuggee during the trap dance:
					// report that stop right after the continue response
					handleWaitOutcome(interrupted);
				}
			default:
				reject(requestSeq, "Cannot continue: debuggee is not stopped");
		}
	}

	// Lets the debuggee run on from a stop. A stop on a breakpoint first runs
	// the trap dance over the original instruction and re-arms the INT3. Returns
	// a pending debug event when one from another thread interrupted the dance:
	// every thread runs during the single step, so a second thread can hit a
	// breakpoint here. The caller must settle its own state and then process
	// that event as a normal stop; resuming past it leaves the process frozen.
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
		// After a user pause, the event to continue belongs to the thread of the
		// forced break, not to the inspected Haxe thread. It is used once.
		var resumeThread = pauseEventThread != -1 ? pauseEventThread : threadId;
		pauseEventThread = -1;
		api.resume(debuggeePid, resumeThread);
		return null;
	}

	// User pause: interrupts the running debuggee and reports a stop with reason
	// "pause". The forced break can land on a thread that is not an HL thread (a
	// system thread on Windows). Its event is kept in pauseEventThread for the
	// resume, and an HL thread is reported for inspection. The user thus sees a
	// Haxe stack, and continue and step resume the right event.
	function handlePause(requestSeq:Int, threadId:Int):Void {
		switch (state) {
			case Running:
				var outcome = forceBreakAndDrain();
				if (outcome == null) {
					settlePauseWithoutForcedStop(requestSeq);
					return;
				}
				pauseEventThread = outcome.threadId;
				var inspectThread = pauseInspectThread(threadId, outcome.threadId);
				enterStopped(inspectThread, null);
				emit(EvPaused(requestSeq));      // ack the pause request first
				emit(EvStoppedPause(inspectThread)); // then the stopped(reason:"pause") event
			case Stopped(_):
				// already stopped (a breakpoint hit as the pause arrived): the client
				// already shows that stop, so only acknowledge
				emit(EvPaused(requestSeq));
			default:
				reject(requestSeq, "Cannot pause: debuggee is not running");
		}
	}

	// Answers a pause request that got no forced stop. When the debuggee
	// exited (EvExited already sent) or stopped by itself while the interrupt
	// was pending (that stop is already reported), the pause is acknowledged.
	// When it is still running, the pause failed.
	function settlePauseWithoutForcedStop(requestSeq:Int):Void {
		switch (state) {
			case Running:
				reject(requestSeq, "Could not pause the debuggee");
			default:
				emit(EvPaused(requestSeq));
		}
	}

	// The HL thread to report a pause on: the requested thread if it is a live
	// HL thread, else the main (first) HL thread, else the thread of the event.
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

	// The trap dance: executes the one instruction under a lifted INT3 by
	// single-stepping the thread with the CPU trap flag. On return the debuggee
	// is frozen again, because the single-step event or an interrupting event is
	// pending; register writes are reliable only then.
	function trapDance(threadId:Int):Null<WaitOutcome> {
		setTrapFlag(threadId);
		api.resume(debuggeePid, threadId);
		var interrupted = waitForSingleStep(threadId);
		// clear the trap flag or the debuggee keeps single-stepping forever
		clearTrapFlag(threadId);
		return interrupted;
	}

	// Waits for the single step on `threadId` to complete. Every other thread
	// runs during that one instruction, so other events can arrive first:
	//  - Handled(4): an event hl_debug_wait already continued (thread create,
	//    exit or rename). It is not a stop, so keep waiting. Treating it as the
	//    step's completion clears the trap flag on running threads, which is
	//    unreliable, and aims the final resume at the wrong event; the
	//    debuggee then freezes at random.
	//  - Breakpoint/Error/StackOverflow from any thread: a real pending event
	//    that now owns the process freeze. It is returned for the caller to
	//    handle as a normal stop. Continuing it with the stepping thread's id
	//    fails and leaves the debuggee frozen.
	function waitForSingleStep(threadId:Int):Null<WaitOutcome> {
		for (_ in 0...100) {
			var outcome = api.wait(debuggeePid, ATTACH_DRAIN_MS);
			switch (outcome.result) {
				case SingleStep:
					if (outcome.threadId == threadId) {
						return null;
					}
					// a leftover single step of another thread: continue it, keep waiting
					api.resume(debuggeePid, outcome.threadId);
				case Handled:
					// already continued inside hl_debug_wait: not a stop
				case Timeout:
					// keep waiting: the instruction hasn't retired yet
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

	// Runs on past a step temp that is not this step's landing (hit by another
	// thread, in a deeper recursive frame, or at a deferred closure call site):
	// single-steps past it, re-arms it and resumes.
	function stepPastTempAndResume(threadId:Int, address:Pointer):Void {
		breakpoints.suspendTemp(address);
		var interrupted = trapDance(threadId);
		breakpoints.rearmTemp(address);
		if (state == Exited) {
			return;
		}
		if (interrupted != null) {
			// A pending event from another thread owns the freeze: process it as a
			// normal stop instead of resuming. The recursion is bounded, because
			// each nested call consumes an event that is already pending.
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

	// Enters the Stopped state on `threadId`: ends any active step, records the
	// breakpoint stopped on (null for a step or exception stop), and prepares
	// the inspector for a new stop. The caller emits the stopped event.
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
				// not stopped: report the thread of the last stop, so the client
				// always has a thread for its views
				emit(EvThreads(requestSeq, [{id: stoppedThreadId == 0 ? 1 : stoppedThreadId, name: "main"}]));
		}
	}

	// The live threads from HL's thread registry. A program compiled without
	// thread support reports a single "main" thread.
	function threadList():Array<ThreadInfo> {
		return threadRegistry.read(jit.threadsPtr, jit.threads, stoppedThreadId);
	}

	function handleStackTrace(requestSeq:Int, threadId:Int):Void {
		switch (state) {
			case Stopped(_):
				var frames:Array<FrameInfo> = [];
				for (frame in inspector.framesFor(threadId)) {
					var location = frame.location;
					var source = module.sourceLineAt(location.fidx, location.op);
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

	function handleDisconnect(requestSeq:Int):Void {
		if (debuggeePid != 0) {
			// Free the debuggee before letting go. Launch mode kills it, since the
			// session owns it. Attach mode must not kill (the client owns the
			// lifetime) and restores every patched INT3 instead: the process may
			// keep running after the detach, and a leftover trap would crash it.
			// Both modes then continue any held stop event, so a suspended process
			// does not stall the detach below.
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
			// Continue any debug events still pending (the stop just continued, or
			// an EXIT_PROCESS from the kill). Windows' DebugActiveProcessStop stalls
			// while an event is outstanding, and the client then hangs on "waiting
			// for process detach".
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
		// Answer before the detach. In attach mode DebugActiveProcessStop can
		// stall for seconds on a debuggee that was just suspended. The client tears
		// the adapter down (killing both processes) as soon as it sees this
		// response, which makes the detach moot. Answering after the detach would
		// leave the client waiting for its disconnect timeout.
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
				// hl_debug_wait already continued this event (thread create, exit or
				// rename, DLL load). Continuing it again fails silently at best, and
				// at worst continues a real event that arrived meanwhile, so do
				// nothing.
				if (outcome.threadId == -1 && HostPlatform.isPtraceBased()) {
					// Except for tid -1 on linux: waitpid() failed (ECHILD) because
					// the debuggee died without a parseable exit status. An INT3 run
					// by an untraced worker thread kills the whole process (linux
					// traces per thread, and only the main thread is attached).
					// debug.c maps that WIFSIGNALED status to this code, and every
					// later wait returns -1, so the loop spins forever unless this
					// counts as the exit. Windows events always carry a real tid; the
					// platform check keeps this branch inert there.
					finishStep();
					state = Exited;
					releaseExitedProcess(outcome.threadId);
					emitExitedAfterOutputDrain();
				}
		}
	}

	// Classifies an INT3 stop by what is patched at the trap address and routes
	// it. A user breakpoint wins over an armed throw site, which wins over the
	// hl_throw entry trap, which wins over a step temp. A trap at no known patch
	// site goes to handleUnpatchedTrap.
	function handleBreakpointHit(threadId:Int):Void {
		// INT3 leaves the instruction pointer one byte past the trap
		var eip = api.readRegister(debuggeePid, threadId, Eip);
		var hitAddress = Int64.sub(eip, Int64.ofInt(1));
		if (breakpoints == null || !breakpoints.isPatchedSite(hitAddress)) {
			handleUnpatchedTrap(threadId);
			return;
		}

		// rewind to the INT3's address, so the original instruction runs on the next resume
		api.writeRegister(debuggeePid, threadId, Eip, hitAddress);

		var userBp = breakpoints.atAddress(hitAddress);
		var throwSite = breakpoints.exceptionAt(hitAddress);
		if (userBp != null) {
			lineBreakpoints.handleHit(threadId, userBp);
		} else if (throwSite != null) {
			exceptions.handleSiteHit(threadId, throwSite);
		} else if (breakpoints.isNativeThrow(hitAddress)) {
			exceptions.handleNativeThrowHit(threadId);
		} else {
			stepping.handleTempHit(threadId, hitAddress);
		}
	}

	// A trap at no known patch site. It is either hl_throw's own hl_debug_break
	// completing a parked VM throw (ExceptionController reports that stop), or
	// a forced break, an attach or loader breakpoint or other stray trap, which
	// is resumed silently.
	function handleUnpatchedTrap(threadId:Int):Void {
		if (exceptions.handleVmThrowBreak(threadId)) {
			return;
		}
		// while a forced break is pending, the stray trap is its stop
		forcedBreakPending = false;
		api.resume(debuggeePid, threadId);
	}

	// Resumes past a trap that must not stop the debuggee (a false breakpoint
	// condition, a filtered exception, a throw the hl_throw entry trap lets
	// through) and processes any stop another thread raised meanwhile.
	function resumePastSuppressedTrap(threadId:Int, bp:PatchedBreakpoint):Void {
		inspector.invalidate();
		var interrupted = stepPastBreakpointAndResume(threadId, bp);
		if (state == Exited) {
			return;
		}
		if (interrupted != null) {
			handleWaitOutcome(interrupted);
		}
	}

	// Single-steps past `bp` and resumes without reporting a stop, leaving
	// currentStoppedBreakpoint and any in-flight step untouched. Returns a
	// pending event when another thread interrupted the trap dance; the caller
	// settles it as a normal stop.
	function stepPastBreakpointAndResume(threadId:Int, bp:PatchedBreakpoint):Null<WaitOutcome> {
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

	// hl_debug_wait returns Exit for the exit-process debug event without
	// continuing it, and Windows keeps the dying process alive until the
	// debugger continues that event and detaches. Releasing it lets the process
	// owner (the client in attach mode, the Process handle in launch mode) see it
	// terminate. Call this before reading the exit code: waitExitCode blocks
	// until the process has fully terminated.
	function releaseExitedProcess(threadId:Int):Void {
		try {
			api.resume(debuggeePid, threadId);
		} catch (e:Dynamic) {}
		try {
			api.stop(debuggeePid);
		} catch (e:Dynamic) {}
	}

	/**
		Forwards the debuggee's remaining output, then emits the exited event. A
		dead process's pipes still hold buffered output, and clients that stop
		listening at exit lose any output event that follows `exited`. The pipes
		reach EOF promptly once the process is dead; the timeout is a guard.
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
	Buffered view of the handshake socket for JitInfoReader. It reads in large
	chunks, because parsing byte by byte off the socket costs one syscall per
	byte. It refills only when the parser needs more bytes, so it never waits
	for data past the final handshake byte. This holds because the handshake is
	self-delimiting and JitInfoReader never reads ahead: a refill only asks for
	bytes the VM has sent or is sending.
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
		// blocks until some data arrives (bounded by the socket timeout) and
		// accepts a partial chunk instead of waiting for CHUNK bytes
		available = source.readBytes(buffer, 0, CHUNK);
		position = 0;
		if (available <= 0) {
			throw new haxe.io.Eof();
		}
	}
}

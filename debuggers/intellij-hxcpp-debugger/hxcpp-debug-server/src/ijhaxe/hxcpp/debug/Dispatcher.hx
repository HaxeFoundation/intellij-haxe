package ijhaxe.hxcpp.debug;

import ijhaxe.dap.protocol.SourceBreakpoint;
import ijhaxe.dap.protocol.requests.SetBreakpointsArguments;
import haxe.Json;
import ijhaxe.hxcpp.debug.DebuggerApi;
import ijhaxe.hxcpp.debug.breakpoints.Breakpoints;
import ijhaxe.hxcpp.debug.breakpoints.LineTable;
import ijhaxe.hxcpp.debug.eval.Evaluator;
import ijhaxe.hxcpp.debug.values.Values;
import ijhaxe.hxcpp.debug.values.VariablesView;

/**
	Answers DAP requests and turns runtime debug events into DAP events. Every
	response and event leaves as a JSON string through the `send` sink; the
	transport adds the framing.

	The class is synchronous and owns no threads or sockets (the Server does),
	so every behaviour is unit-testable over a fake DebuggerApi.
**/
class Dispatcher {
	final debugger:DebuggerApi;
	final send:String->Void;
	final breakpoints:Breakpoints;
	final variablesView:VariablesView;
	final evaluator:Evaluator;

	var nextSeq:Int = 1;
	var nextBreakpointId:Int = 1;

	/** Set once a disconnect request was answered; the Server shuts down. */
	public var shutdownRequested(default, null):Bool = false;

	/** Set once the client finished configuration (breakpoints may arrive before). */
	public var configurationDone(default, null):Bool = false;

	// No debug event may precede the `initialized` event. A debuggee whose
	// threads already exist fires THREAD_CREATED as soon as debugging is
	// enabled, before the client has even sent initialize. Such events are
	// buffered until initialize is handled, then sent in arrival order.
	var initialized:Bool = false;
	final pendingEvents:Array<DebugEvent> = [];
	// The thread most recently reported stopped. hxcpp's continueThreads needs
	// that specific thread as its "special" argument, not a wildcard.
	var lastStoppedThread:Int = -1;
	// The stack (innermost last) of every currently stopped thread, captured
	// on that thread. A pause stops all threads, and the client then asks for
	// each one's stackTrace. An entry is removed when its thread resumes.
	final stoppedStacks = new Map<Int, Array<DebugStackFrame>>();
	// DAP frameId -> (thread, hxcpp stack index). The id must identify the
	// thread because scopes and evaluate receive only a frameId. Ids stay valid
	// until the next resume, so the stop events of other threads in the same
	// pause do not invalidate them.
	var nextFrameId:Int = 1;
	final framesById = new Map<Int, {thread:Int, index:Int}>();

	// A source-level step in flight. hxcpp stops again on the SAME line when a
	// line holds several expressions, and it never re-fires a loop-body line.
	// A step is therefore re-issued until the source line changes, up to
	// MAX_STEP_ITERATIONS so a pathological program cannot step forever.
	static inline var MAX_STEP_ITERATIONS = 100000;
	var stepActive:Bool = false;
	var stepType:Int = 0;
	var stepFromFile:String = "";
	var stepFromLine:Int = 0;
	var stepIterations:Int = 0;

	// Smart step into (the custom request "custom/stepIntoFunction"). A
	// TEMPORARY class-function breakpoint at the chosen callee's entry races a
	// STEP_OVER; whichever lands first is the stop, reported as a plain step.
	// -1 means none is armed. The temporary breakpoint has no DAP id and no
	// condition, and it is deleted at the next reported stop or resume.
	var tempStepBreakpoint:Int = -1;

	// How many callee entries smart step into still skips. A callee called
	// MORE THAN ONCE on the line (`cfg.test1(1)...test1(2)`) hits the entry
	// breakpoint at its FIRST call, whichever call was chosen. The request's
	// optional 1-based `occurrence` therefore says how many entries to skip.
	// A skipped entry steps OUT with the temporary breakpoint still armed.
	// hxcpp's step-out never lands back on a one-line chain, because the rest
	// of the chain carries no line marker, so the next call's entry hit is
	// what interrupts the OUT. Known limit: when a skipped call recursively
	// calls the same function, the remaining skips are used up too early.
	var tempStepSkipsRemaining:Int = 0;

	// Exception filters. Both default ON, matching the defaults advertised in
	// initialize, so a client that never sends setExceptionBreakpoints gets
	// them.
	static inline var FILTER_UNCAUGHT = "uncaught";
	static inline var FILTER_CRITICAL = "critical";

	// The "thrown exceptions" filter. The runtime has no hook for a CATCHABLE
	// throw. But every `new haxe.Exception(...)`, including every subclass
	// constructor through super(), runs haxe.Exception.new, and idiomatic code
	// constructs the exception in the throw expression. A class-function
	// breakpoint on that constructor, the "thrown hook", therefore breaks
	// where any haxe.Exception is thrown. Raw-value throws (`throw "str"`)
	// never reach it.
	static inline var FILTER_THROWN = "thrown";
	static inline var THROWN_HOOK_CLASS = "haxe.Exception";

	var breakOnUncaught:Bool = true;
	var breakOnCritical:Bool = true;
	var breakOnThrown:Bool = false;

	// Typed exception filters (DAP filterTypes): the class names to stop on.
	// A stop at the thrown hook reads the CONCRETE class from `this` and
	// matches it and its superclasses. A subclass therefore matches its base
	// class's filter, even when it inherits its constructor and has no `new`
	// frame of its own.
	var thrownTypeFilters:Array<String> = [];
	var thrownHookBreakpoint:Int = -1; // installed while the thrown or a typed filter is on

	// the last exception stop, kept for the exceptionInfo request
	var lastExceptionDescription:Null<String> = null;
	var lastExceptionKind:Null<String> = null;

	// Resuming a critical error usually faults again at once: the runtime's
	// "fixup" path re-executes the null access, which faults and stops again.
	// Silently resuming with the filter off would therefore livelock the
	// program, so after this many consecutive silent resumes the stop is
	// reported anyway.
	static inline var MAX_SILENT_CRITICAL_RESUMES = 3;
	var silentCriticalResumes:Int = 0;

	public function new(debugger:DebuggerApi, send:String->Void) {
		this.debugger = debugger;
		this.send = send;
		// the executable-line table compiled into this binary, if any
		this.breakpoints = new Breakpoints(debugger, LineTable.fromResource());
		this.variablesView = new VariablesView(debugger);
		this.evaluator = new Evaluator(debugger);
	}

	/**
		Handles one request payload. Never throws on bad input: a broken request
		gets a failure response and the session lives on.
	**/
	public function handleRequest(payload:String):Void {
		var request:Dynamic = try {
			Json.parse(payload);
		} catch (e:Dynamic) {
			sendResponse(0, "", false, null, "Invalid JSON payload");
			return;
		}
		var command:String = request.command;
		var seq:Int = request.seq != null ? request.seq : 0;
		if (request.type != "request" || command == null) {
			sendResponse(seq, command == null ? "" : command, false, null, "Not a valid DAP request");
			return;
		}
		try {
			dispatch(seq, command, request);
		} catch (e:Dynamic) {
			// Fault isolation: one faulting handler must not end the session.
			// Reading a corrupt frame slot raises a critical error, which hxcpp
			// re-throws on this debug thread as "Critical Error in the debugger
			// thread". Uncaught, it would unwind into the Server's
			// connection-lost handler and the server would silently stop
			// serving. A hard segfault still kills the process.
			sendResponse(seq, command, false, null, "Internal debugger error: " + Std.string(e));
		}
	}

	function dispatch(seq:Int, command:String, request:Dynamic):Void {
		switch (command) {
			case "initialize":
				sendResponse(seq, command, true, {
					supportsConfigurationDoneRequest: true,
					supportsConditionalBreakpoints: true,
					supportsEvaluateForHovers: true,
					supportsSetVariable: true,
					supportsExceptionInfoRequest: true,
					// The runtime reports uncaught throws and critical errors both as
					// CRITICAL_ERROR stops; exceptionKind tells them apart by their
					// description. "Break on caught exceptions" has no runtime hook
					// (see docs/README.md).
					exceptionBreakpointFilters: [
						{
							filter: FILTER_UNCAUGHT,
							label: "Uncaught exceptions",
							description: "Break where a value is thrown that no enclosing try/catch can catch (before unwinding).",
							'default': true
						},
						{
							filter: FILTER_CRITICAL,
							label: "Critical errors",
							description: "Break on runtime critical errors (null access, GC errors). Under a debugger these stop even inside try/catch.",
							'default': true
						},
						{
							filter: FILTER_THROWN,
							label: "Thrown exceptions (haxe.Exception)",
							description: "Break where a haxe.Exception (or subclass) is constructed - normally the throw expression - even when it will be caught. Raw-value throws (strings, enums) are not visible to this filter.",
							'default': false
						}
					]
				});
				// the spec requires the initialized event strictly after the response
				sendEvent("initialized", null);
				initialized = true;
				for (event in pendingEvents) {
					emitDebugEvent(event);
				}
				pendingEvents.resize(0);
			case "setBreakpoints":
				handleSetBreakpoints(seq, command, request.arguments);
			case "setExceptionBreakpoints":
				handleSetExceptionBreakpoints(seq, command, request.arguments);
			case "exceptionInfo":
				handleExceptionInfo(seq, command);
			case "continue":
				// hxcpp's continueThreads takes the stopped thread as its "special"
				// argument, not a wildcard; count 1 stops at the next breakpoint.
				// It resumes EVERY stopped thread, hence allThreadsContinued.
				// The response goes out BEFORE the threads are released: a resumed
				// program can run to exit() before this dispatcher runs again, and
				// an unwritten response dies with the process's socket.
				stepActive = false;
				clearTempStepBreakpoint();
				invalidateReferences();
				stoppedStacks.clear();
				sendResponse(seq, command, true, {allThreadsContinued: true});
				debugger.continueThreads(requestedThread(request.arguments), 1);
			case "custom/stepIntoFunction":
				handleStepIntoFunction(seq, command, request.arguments);
			case "custom/setToStringRendering":
				// Switches toString object labels on or off (see
				// Values.objectLabel). The client then re-requests the variables,
				// so the current stop's rows are described with the new labels.
				Values.renderWithToString = request.arguments != null && request.arguments.enabled == true;
				sendResponse(seq, command, true, null);
			case "pause":
				// stops every thread; with no step in flight, the resulting
				// BREAK_IMMEDIATE stop is reported as reason "pause"
				stepActive = false;
				debugger.breakNow(false);
				sendResponse(seq, command, true, null);
			case "next":
				handleStep(seq, command, request.arguments, StepType.OVER);
			case "stepIn":
				handleStep(seq, command, request.arguments, StepType.INTO);
			case "stepOut":
				handleStep(seq, command, request.arguments, StepType.OUT);
			case "stackTrace":
				handleStackTrace(seq, command, request.arguments);
			case "scopes":
				handleScopes(seq, command, request.arguments);
			case "variables":
				var reference = (request.arguments != null && request.arguments.variablesReference != null)
					? request.arguments.variablesReference : 0;
				sendResponse(seq, command, true, {variables: variablesView.variables(reference)});
			case "setVariable":
				handleSetVariable(seq, command, request.arguments);
			case "evaluate":
				handleEvaluate(seq, command, request.arguments);
			case "configurationDone":
				configurationDone = true;
				sendResponse(seq, command, true, null);
			case "threads":
				sendResponse(seq, command, true, {
					threads: [for (t in debugger.threads()) {id: t.number, name: "Thread " + t.number}]
				});
			case "disconnect":
				sendResponse(seq, command, true, null);
				shutdownRequested = true;
			default:
				sendResponse(seq, command, false, null, "Unrecognized command: " + command);
		}
	}

	// The thread a continue, step or stackTrace request acts on: the request's
	// threadId, else the last stopped thread. A DAP client always names one.
	function requestedThread(args:Dynamic):Int {
		return (args != null && args.threadId != null) ? args.threadId : lastStoppedThread;
	}

	// Called on every resume: variable references and frame ids must not
	// outlive the stop that created them.
	function invalidateReferences():Void {
		variablesView.reset();
		framesById.clear();
		nextFrameId = 1;
	}

	// Begins a source-level step. It records the current line so that
	// handleThreadStopped can re-step until the line changes. The response goes
	// out at once; the stopped(reason:"step") event follows when the step lands.
	function handleStep(seq:Int, command:String, args:Dynamic, type:Int):Void {
		clearTempStepBreakpoint(); // a new user step cancels a pending smart step

		var threadId = requestedThread(args);
		var from = topFrame(stoppedStacks.get(threadId));
		stepActive = true;
		stepType = type;
		stepFromFile = from != null ? from.fileName : "";
		stepFromLine = from != null ? from.lineNumber : 0;
		stepIterations = 0;

		invalidateReferences();
		stoppedStacks.remove(threadId); // stepThread resumes only this thread
		// Respond BEFORE releasing the thread: a step off the program's last
		// line exits the process, and an unwritten response dies with it.
		sendResponse(seq, command, true, null);
		debugger.stepThread(threadId, type);
	}

	/**
		Smart step into: enters the call on the stopped line that the user
		CHOSE. The IDE finds the line's calls in its PSI and names the callee as
		(className, functionName). The server cannot list a line's calls itself,
		because hxcpp has no bytecode to inspect. A temporary breakpoint at the callee's entry races an
		ordinary step-over. Entering the callee hits that breakpoint, after any
		earlier calls on the line have run. If the chosen call never executes
		(short-circuit, conditional), the step-over lands instead, so the
		request degrades to a plain step over, as in the HashLink debugger.
		Either landing is reported as reason "step".
	**/
	function handleStepIntoFunction(seq:Int, command:String, args:Dynamic):Void {
		if (args == null || args.className == null || args.functionName == null) {
			sendResponse(seq, command, false, null, "Missing className/functionName");
			return;
		}
		clearTempStepBreakpoint(); // replaces any pending smart step
		var threadId = requestedThread(args);
		var from = topFrame(stoppedStacks.get(threadId));
		var number = debugger.addClassFunctionBreakpoint(args.className, args.functionName);
		if (number < 0) {
			// The runtime rejected the class name: hxcpp checks it against its
			// compiled-in class table, and -1 arms nothing. Without any live
			// breakpoint, hxcpp skips its per-line check, so a step could run
			// unchecked to the end of the program (see "Stepping needs at least
			// one breakpoint armed" in docs/README.md). The thread is therefore
			// not resumed; the current stop is re-reported instead, a visible
			// no-op the user can follow with a plain step.
			sendResponse(seq, command, true, null);
			sendEvent("stopped", {reason: "step", threadId: threadId, allThreadsStopped: true});
			return;
		}
		tempStepBreakpoint = number;
		// The chosen call of the callee on this line, 1-based; clients that
		// omit it mean the first. The entries before it are skipped.
		var occurrence:Null<Int> = args.occurrence;
		tempStepSkipsRemaining = occurrence != null && occurrence > 1 ? occurrence - 1 : 0;
		// Set up the step state exactly like a step-over, so the same-line
		// re-step keeps the step racing while the temporary breakpoint is armed.
		stepActive = true;
		stepType = StepType.OVER;
		stepFromFile = from != null ? from.fileName : "";
		stepFromLine = from != null ? from.lineNumber : 0;
		stepIterations = 0;
		invalidateReferences();
		stoppedStacks.remove(threadId);
		// respond BEFORE releasing the thread, for the same reason as handleStep
		sendResponse(seq, command, true, null);
		debugger.stepThread(threadId, StepType.OVER);
	}

	function clearTempStepBreakpoint():Void {
		if (tempStepBreakpoint >= 0) {
			debugger.deleteBreakpoint(tempStepBreakpoint);
			tempStepBreakpoint = -1;
		}
	}

	function handleStackTrace(seq:Int, command:String, args:Dynamic):Void {
		var threadId = requestedThread(args);
		var stack = stoppedStacks.get(threadId);
		var frames:Array<Dynamic> = [];
		if (stack != null) {
			// hxcpp orders the stack innermost LAST and DAP wants the newest
			// frame first, so the stack is walked in reverse. Each frame gets an
			// id that maps back to its (thread, index), because scopes and
			// evaluate receive only the id.
			var i = stack.length - 1;
			while (i >= 0) {
				var frame = stack[i];
				var id = nextFrameId++;
				framesById.set(id, {thread: threadId, index: i});
				var source:Dynamic = {name: baseName(frame.fileName), path: fullPathFor(frame.fileName)};
				frames.push({
					id: id,
					name: frame.className + "." + frame.functionName,
					line: frame.lineNumber,
					column: 1,
					source: source
				});
				i--;
			}
		}
		sendResponse(seq, command, true, {stackFrames: frames, totalFrames: frames.length});
	}

	// Reports a frame's single "Locals" scope. The frameId maps to a (thread,
	// stack index) pair recorded by stackTrace. hxcpp exposes one flat set of
	// locals per frame (parameters, declared variables and `this`), so the
	// arguments are not split into a scope of their own.
	function handleScopes(seq:Int, command:String, args:Dynamic):Void {
		var frameId = (args != null && args.frameId != null) ? args.frameId : 0;
		var location = framesById.get(frameId);
		if (location == null) {
			sendResponse(seq, command, false, null, "Unknown or stale frameId " + frameId);
			return;
		}
		var reference = variablesView.frameScope(location.thread, location.index);
		sendResponse(seq, command, true, {
			scopes: [{name: "Locals", variablesReference: reference, expensive: false}]
		});
	}

	// Evaluates a watch, hover or console expression against a frame; a bare
	// assignment writes back to the debuggee. The frameId comes from
	// stackTrace; without one, the last stop's innermost frame is used.
	function handleEvaluate(seq:Int, command:String, args:Dynamic):Void {
		if (args == null || args.expression == null) {
			sendResponse(seq, command, false, null, "Missing expression");
			return;
		}
		var location = (args.frameId != null) ? framesById.get(args.frameId) : null;
		var thread = location != null ? location.thread : lastStoppedThread;
		var frame;
		if (location != null) {
			frame = location.index;
		} else {
			// the innermost frame has the highest hxcpp index
			var stack = stoppedStacks.get(lastStoppedThread);
			frame = stack != null ? stack.length - 1 : 0;
		}
		try {
			var value = evaluator.evaluate(thread, frame, args.expression);
			var presented = variablesView.present(value);
			sendResponse(seq, command, true, {result: presented.value, type: presented.type, variablesReference: presented.variablesReference});
		} catch (e:Dynamic) {
			sendResponse(seq, command, false, null, Std.string(e));
		}
	}

	function handleSetVariable(seq:Int, command:String, args:Dynamic):Void {
		if (args == null || args.variablesReference == null || args.name == null || args.value == null) {
			sendResponse(seq, command, false, null, "Missing variablesReference, name or value");
			return;
		}
		var result = variablesView.setVariable(args.variablesReference, args.name, args.value);
		if (result == null) {
			sendResponse(seq, command, false, null, "Cannot set '" + args.name + "': unknown or stale reference");
			return;
		}
		sendResponse(seq, command, true, result);
	}

	// DAP replaces the whole breakpoint set of a source. Each requested
	// breakpoint gets a new DAP id, and Breakpoints installs the batch.
	function handleSetBreakpoints(seq:Int, command:String, args:SetBreakpointsArguments):Void {
		var sourcePath = (args != null && args.source != null && args.source.path != null) ? args.source.path : "";
		var requested:Array<SourceBreakpoint> = (args != null && args.breakpoints != null) ? args.breakpoints : [];
		var ids = [for (_ in requested) nextBreakpointId++];
		var results = breakpoints.setForSource(sourcePath, requested, ids);
		sendResponse(seq, command, true, {breakpoints: results});
	}

	/**
		Turns a runtime notification, already handed over to the server thread,
		into the matching DAP event. Before initialize is handled, the
		notification is buffered, so that nothing precedes the `initialized`
		event.
	**/
	public function handleDebugEvent(event:DebugEvent):Void {
		if (!initialized) {
			pendingEvents.push(event);
			return;
		}
		emitDebugEvent(event);
	}

	function emitDebugEvent(event:DebugEvent):Void {
		switch (event) {
			case ThreadCreated(threadNumber):
				sendEvent("thread", {reason: "started", threadId: threadNumber});
			case ThreadTerminated(threadNumber):
				sendEvent("thread", {reason: "exited", threadId: threadNumber});
			case ThreadStarted(threadNumber):
				// The runtime's "started" means the thread RESUMED, so its stack
				// is stale. DAP needs no event: the continue and step responses
				// already imply the resume.
				stoppedStacks.remove(threadNumber);
			case ThreadStopped(threadNumber, status, breakpoint, stack, description):
				handleThreadStopped(threadNumber, status, breakpoint, stack, description);
		}
	}

	function handleThreadStopped(threadNumber:Int, status:Int, breakpoint:Int, stack:Array<DebugStackFrame>, description:Null<String>):Void {
		lastStoppedThread = threadNumber;
		stoppedStacks.set(threadNumber, stack);
		// References are NOT invalidated here. A pause stops every thread, and
		// their stop events arrive in quick succession; invalidating on each one
		// would break the frame ids the client just received for another
		// thread. invalidateReferences() runs on resume instead.
		lastExceptionDescription = null;
		lastExceptionKind = null;

		// A step landed without changing the source line (a line with several
		// expressions, or a loop-body line that never "changes"): re-issue the
		// step, up to MAX_STEP_ITERATIONS. This applies only to a plain step
		// landing (BREAK_IMMEDIATE); a breakpoint or exception hit during the
		// step takes precedence and is reported.
		if (stepActive && status == DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE) {
			var top = topFrame(stack);
			if (top != null && top.fileName == stepFromFile && top.lineNumber == stepFromLine
					&& stepIterations < MAX_STEP_ITERATIONS) {
				stepIterations++;
				stoppedStacks.remove(threadNumber);
				debugger.stepThread(threadNumber, stepType);
				return; // keep stepping; no stopped event yet
			}
			stepActive = false;
			clearTempStepBreakpoint(); // the step-over won the smart-step race
			sendEvent("stopped", {reason: "step", threadId: threadNumber, allThreadsStopped: true});
			return;
		}

		// Any other stop ends a pending step.
		stepActive = false;

		// A stop at the smart-step breakpoint is the chosen callee's entry and
		// is reported as a plain step. Any OTHER stop (user breakpoint,
		// exception, pause) wins the race and is reported normally. Either way
		// the temporary breakpoint is deleted here.
		if (tempStepBreakpoint >= 0) {
			var enteredTarget = status == DebugThread.STATUS_STOPPED_BREAKPOINT && breakpoint == tempStepBreakpoint;
			if (enteredTarget && tempStepSkipsRemaining > 0) {
				// A LATER call of this callee was chosen, so this entry is not
				// the target. Step OUT with the temporary breakpoint still armed:
				// the next call's entry interrupts the OUT. The OUT cannot land
				// back on a one-line chain, because the rest of the chain carries
				// no line marker. If no further call happens, the OUT lands on
				// the next line, the same fallback as a chosen call that never
				// runs.
				tempStepSkipsRemaining--;
				stepActive = true;
				stoppedStacks.remove(threadNumber);
				debugger.stepThread(threadNumber, StepType.OUT);
				return;
			}
			clearTempStepBreakpoint();
			if (enteredTarget) {
				sendEvent("stopped", {reason: "step", threadId: threadNumber, allThreadsStopped: true});
				return;
			}
		}

		// A stop at the thrown hook: haxe.Exception.new is running, so an
		// exception is being constructed, normally by a throw expression. It is
		// reported when the "thrown" filter is on, or when the concrete class or
		// one of its superclasses matches a typed filter. Otherwise the thread
		// resumes silently; the hook is also armed for typed filters alone.
		if (thrownHookBreakpoint >= 0 && status == DebugThread.STATUS_STOPPED_BREAKPOINT && breakpoint == thrownHookBreakpoint) {
			var classChain = thrownClassChain(threadNumber, stack);
			if (!breakOnThrown && !matchesTypeFilter(classChain)) {
				invalidateReferences();
				stoppedStacks.clear(); // continueThreads resumes every thread
				debugger.continueThreads(threadNumber, 1);
				return;
			}
			// the text reads the constructor frame's locals, so it is built BEFORE trimming
			var text = thrownExceptionText(threadNumber, stack, classChain);
			// Trim the exception's OWN constructor frames (haxe.Exception.new and
			// any subclass constructor chaining to it), so the top frame reported
			// is the THROW SITE. Only constructors of the exception's class chain
			// are trimmed; a user constructor that itself throws stays visible.
			// Trimming only the innermost end keeps the remaining hxcpp frame
			// indices valid for scopes and evaluate.
			var end = stack.length;
			while (end > 1) {
				var frame = stack[end - 1];
				var ownCtor = frame.functionName == "new"
					&& (frame.className == THROWN_HOOK_CLASS || classChain.indexOf(frame.className) >= 0);
				if (!ownCtor) {
					break;
				}
				end--;
			}
			stoppedStacks.set(threadNumber, stack.slice(0, end));
			lastExceptionDescription = text;
			lastExceptionKind = FILTER_THROWN;
			sendEvent("stopped", {
				reason: "exception",
				threadId: threadNumber,
				allThreadsStopped: true,
				description: "Thrown exception",
				text: text
			});
			return;
		}

		// An uncaught-exception or critical-error stop. The thread is blocked AT
		// the throw site, before unwinding, so the full stack and the locals can
		// be inspected. With the filter disabled the thread resumes silently,
		// and the runtime unwinds or terminates as if it had never stopped.
		if (status == DebugThread.STATUS_STOPPED_UNCAUGHT_EXCEPTION || status == DebugThread.STATUS_STOPPED_CRITICAL_ERROR) {
			var kind = exceptionKind(description);
			var enabled = kind == FILTER_UNCAUGHT ? breakOnUncaught : breakOnCritical;
			if (!enabled) {
				// Resuming an uncatchable throw unwinds or terminates cleanly. A
				// critical error faults again, so its silent resumes are capped.
				if (kind != FILTER_CRITICAL || silentCriticalResumes < MAX_SILENT_CRITICAL_RESUMES) {
					if (kind == FILTER_CRITICAL) {
						silentCriticalResumes++;
					}
					invalidateReferences();
					stoppedStacks.clear(); // continueThreads resumes every thread
					debugger.continueThreads(threadNumber, 1);
					return;
				}
			}
			silentCriticalResumes = 0;
			lastExceptionDescription = description != null ? description : "Exception";
			lastExceptionKind = kind;
			sendEvent("stopped", {
				reason: "exception",
				threadId: threadNumber,
				allThreadsStopped: true,
				description: kind == FILTER_UNCAUGHT ? "Uncaught exception" : "Critical error",
				text: lastExceptionDescription
			});
			return;
		}

		// A conditional breakpoint stops only when its condition is true; on a
		// false condition the thread resumes silently. The condition is
		// evaluated against the INNERMOST frame, which has the highest hxcpp
		// frame index.
		if (status == DebugThread.STATUS_STOPPED_BREAKPOINT && breakpoint >= 0) {
			var condition = breakpoints.conditionForRuntimeNumber(breakpoint);
			if (condition != null && condition != "" && !evaluator.conditionHolds(threadNumber, stack.length - 1, condition)) {
				invalidateReferences();
				stoppedStacks.clear(); // continueThreads resumes every thread
				debugger.continueThreads(threadNumber, 1);
				return;
			}
		}

		silentCriticalResumes = 0; // any reported stop means the program moved on
		var body:Dynamic = {
			reason: stopReason(status),
			threadId: threadNumber,
			allThreadsStopped: true
		};
		if (breakpoint >= 0) {
			var id = breakpoints.idForRuntimeNumber(breakpoint);
			if (id >= 0) {
				body.hitBreakpointIds = [id];
			}
		}
		sendEvent("stopped", body);
	}

	// Maps a STATUS_* value to the DAP stopped reason. A BREAK_IMMEDIATE that is
	// not a step landing is a user pause. Exception statuses never get here.
	static function stopReason(status:Int):String {
		return switch (status) {
			case DebugThread.STATUS_STOPPED_BREAKPOINT: "breakpoint";
			default: "pause";
		}
	}

	// The filter an exception stop belongs to. The runtime reports both kinds
	// as CRITICAL_ERROR; hxcpp 4.3.2 never emits
	// STATUS_STOPPED_UNCAUGHT_EXCEPTION. An uncatchable user throw is recognized
	// by the "Uncatchable Throw: <value>" prefix that checkedThrow gives it.
	static function exceptionKind(description:Null<String>):String {
		return (description != null && StringTools.startsWith(description, "Uncatchable Throw"))
			? FILTER_UNCAUGHT : FILTER_CRITICAL;
	}

	// DAP sends the complete list of ACTIVE filters each time; an omitted
	// filter is off. filterTypes, a non-standard argument shared with the
	// HashLink adapter, lists the class names of typed exception breakpoints.
	function handleSetExceptionBreakpoints(seq:Int, command:String, args:Dynamic):Void {
		var filters:Array<String> = (args != null && args.filters != null) ? args.filters : [];
		breakOnUncaught = filters.indexOf(FILTER_UNCAUGHT) >= 0;
		breakOnCritical = filters.indexOf(FILTER_CRITICAL) >= 0;
		breakOnThrown = filters.indexOf(FILTER_THROWN) >= 0;
		thrownTypeFilters = (args != null && args.filterTypes != null) ? args.filterTypes : [];
		var hookWanted = breakOnThrown || thrownTypeFilters.length > 0;
		if (hookWanted && thrownHookBreakpoint < 0) {
			// -1 means haxe.Exception is not compiled into this program, because
			// nothing constructs one. The filter then has no effect and is
			// reported unverified.
			thrownHookBreakpoint = debugger.addClassFunctionBreakpoint(THROWN_HOOK_CLASS, "new");
		} else if (!hookWanted && thrownHookBreakpoint >= 0) {
			debugger.deleteBreakpoint(thrownHookBreakpoint);
			thrownHookBreakpoint = -1;
		}
		sendResponse(seq, command, true, {breakpoints: [
			for (filter in filters) {verified: filter != FILTER_THROWN || thrownHookBreakpoint >= 0}
		]});
	}

	function handleExceptionInfo(seq:Int, command:String):Void {
		if (lastExceptionDescription == null) {
			sendResponse(seq, command, false, null, "Not stopped on an exception");
			return;
		}
		sendResponse(seq, command, true, {
			exceptionId: switch (lastExceptionKind) {
				case FILTER_UNCAUGHT: "Uncaught exception";
				case FILTER_THROWN: "Thrown exception";
				case _: "Critical error";
			},
			description: lastExceptionDescription,
			// An uncaught throw cannot be handled. Critical errors and thrown
			// exceptions stop even inside a try/catch, hence "always".
			breakMode: lastExceptionKind == FILTER_UNCAUGHT ? "unhandled" : "always"
		});
	}

	// The dotted class names of the exception under construction: its
	// concrete class first, then its superclasses. The concrete class comes
	// from `this` in the haxe.Exception.new frame, which a subclass
	// constructor reaches through super(). A corrupt frame yields just
	// haxe.Exception.
	function thrownClassChain(threadNumber:Int, stack:Array<DebugStackFrame>):Array<String> {
		return try {
			var frame = stack.length - 1; // innermost = haxe.Exception.new
			var self:Dynamic = debugger.stackVariableValue(threadNumber, frame, "this");
			var cls = Type.getClass(self);
			var chain = [];
			while (cls != null) {
				chain.push(Type.getClassName(cls));
				cls = Type.getSuperClass(cls);
			}
			chain.length > 0 ? chain : [THROWN_HOOK_CLASS];
		} catch (e:Dynamic) {
			[THROWN_HOOK_CLASS];
		}
	}

	// A typed filter matches the concrete class or any superclass, by dotted
	// or bare class name. A "MyBase" filter therefore also stops subclass throws.
	function matchesTypeFilter(classChain:Array<String>):Bool {
		for (name in classChain) {
			if (thrownTypeFilters.indexOf(name) >= 0) {
				return true;
			}
			var shortName = name.substr(name.lastIndexOf(".") + 1);
			if (thrownTypeFilters.indexOf(shortName) >= 0) {
				return true;
			}
		}
		return false;
	}

	// The "<ConcreteClass>: <message>" text of a thrown-hook stop; the message
	// is the constructor's parameter. A corrupt frame must not fail the stop,
	// so any read error falls back to a generic text.
	function thrownExceptionText(threadNumber:Int, stack:Array<DebugStackFrame>, classChain:Array<String>):String {
		return try {
			var frame = stack.length - 1; // innermost = haxe.Exception.new
			var message:Dynamic = debugger.stackVariableValue(threadNumber, frame, "message");
			classChain[0] + ": " + Std.string(message);
		} catch (e:Dynamic) {
			"Thrown exception";
		}
	}

	static inline function topFrame(stack:Null<Array<DebugStackFrame>>):Null<DebugStackFrame> {
		return (stack == null || stack.length == 0) ? null : stack[stack.length - 1]; // innermost is last
	}

	// Runtime file key (the short "Main.hx") -> absolute path, built from the
	// index-aligned files() and filesFullPath() tables on first use.
	var fullPaths:Null<Map<String, String>> = null;

	function fullPathFor(fileKey:String):String {
		if (fullPaths == null) {
			fullPaths = new Map();
			var files = debugger.files();
			var paths = debugger.filesFullPath();
			for (i in 0...files.length) {
				if (i < paths.length) {
					fullPaths.set(files[i], paths[i]);
				}
			}
		}
		var path = fullPaths.get(fileKey);
		return path != null ? path : fileKey;
	}

	static function baseName(path:String):String {
		var normalized = StringTools.replace(path, "\\", "/");
		var slash = normalized.lastIndexOf("/");
		return slash < 0 ? normalized : normalized.substr(slash + 1);
	}

	function sendResponse(requestSeq:Int, command:String, success:Bool, body:Dynamic, ?message:String):Void {
		var response:Dynamic = {
			seq: nextSeq++,
			type: "response",
			request_seq: requestSeq,
			success: success,
			command: command
		};
		if (body != null) {
			response.body = body;
		}
		if (message != null) {
			response.message = message;
		}
		send(Json.stringify(response));
	}

	function sendEvent(name:String, body:Dynamic):Void {
		var event:Dynamic = {
			seq: nextSeq++,
			type: "event",
			event: name
		};
		if (body != null) {
			event.body = body;
		}
		send(Json.stringify(event));
	}
}

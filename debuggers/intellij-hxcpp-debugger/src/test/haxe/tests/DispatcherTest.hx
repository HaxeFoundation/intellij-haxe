package tests;

import haxe.Json;
import ijhaxe.hxcpp.debug.DebuggerApi;
import ijhaxe.hxcpp.debug.Dispatcher;

// An exception hierarchy for the typed-filter tests. SubError inherits its
// constructor — the case a per-type entry breakpoint could never catch, and
// exactly what the this-chain matching exists for.
private class BaseError {
	public var message:String;

	public function new(message:String) {
		this.message = message;
	}
}

private class SubError extends BaseError {}

private class UnrelatedError {
	public var message:String = "nope";

	public function new() {}
}

class DispatcherTest {
	public static function run(assert:Assert):Void {
		initializeRespondsThenEmitsInitialized(assert);
		threadsComeFromTheDebuggerApi(assert);
		disconnectAcksAndRequestsShutdown(assert);
		unknownCommandFailsWithoutKillingTheSession(assert);
		invalidJsonFailsGracefully(assert);
		runtimeEventsMapToDapEvents(assert);
		setBreakpointsInstallsAndReturnsResults(assert);
		aBreakpointStopCarriesTheHitId(assert);
		continueResumesAllThreads(assert);
		eventsBeforeInitializeAreBufferedThenFlushed(assert);
		pauseBreaksTheWorld(assert);
		stepIssuesTheRightStepTypeAndReportsStep(assert);
		aStepThatKeepsTheSameLineReSteps(assert);
		stackTraceReportsFramesNewestFirst(assert);
		aBreakpointHitMidStepWinsOverTheStep(assert);
		evaluateReturnsAResult(assert);
		aFalseConditionResumesWithoutStopping(assert);
		aTrueConditionStops(assert);
		anUncaughtThrowStopsAsException(assert);
		aCriticalErrorStopsAsException(assert);
		aDisabledFilterResumesSilently(assert);
		exceptionInfoDescribesTheLastStop(assert);
		exceptionInfoFailsWhenNotAtAnExceptionStop(assert);
		repeatedSilentCriticalResumesBreakTheLivelock(assert);
		aCorruptLocalPoisonsOneRowNotTheRequest(assert);
		aFaultingHandlerAnswersAndTheSessionLivesOn(assert);
		theThrownFilterInstallsAndRemovesTheHook(assert);
		aThrownHookStopReportsTheExceptionWithItsMessage(assert);
		aMissingExceptionClassMakesTheThrownFilterUnverified(assert);
		aTypedFilterAloneInstallsTheHook(assert);
		aTypedFilterStopsMatchesAndResumesOthers(assert);
		aBaseClassFilterMatchesSubclassThrows(assert);
		aThrownStopTrimsTheExceptionsOwnCtorFramesOnly(assert);
		smartStepEntersTheChosenCallee(assert);
		smartStepFallsBackToStepOver(assert);
		aUserBreakpointWinsTheSmartStepRace(assert);
		aRejectedClassNameIsANoOpStopNotARunaway(assert);
	}

	// hxcpp validates the class name against its class table and returns -1
	// for an unknown one, arming NOTHING — stepping anyway could run unchecked
	// forever (gotcha 8). The server must not resume: it re-reports the
	// current stop instead.
	static function aRejectedClassNameIsANoOpStopNotARunaway(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.api.knownClasses = ["ChainTarget"];
		smartStepRequest(t); // requests class "my.pack.Target": rejected
		assert.isTrue(t.sent[0].success, "the request is acknowledged");
		assert.equals("step", t.sent[1].body.reason, "the current stop is re-reported as a step");
		assert.equals(0, t.api.stepCalls.length, "the thread was NOT resumed");
		assert.equals(0, t.api.installedFunctionBreakpoints.length, "no temp installed");
	}

	static function setTypedFilters(t, filters:Array<String>, types:Array<String>):Void {
		t.dispatcher.handleRequest(Json.stringify({
			seq: 2, type: "request", command: "setExceptionBreakpoints",
			arguments: {filters: filters, filterTypes: types}
		}));
	}

	// simulate a thrown-hook stop with `this` being the given exception object
	static function hookStop(t:{api:FakeDebuggerApi, dispatcher:Dispatcher}, hook:Int, self:Dynamic, message:String):Void {
		t.api.localNames = ["this", "message"];
		t.api.localValues.set("this", self);
		t.api.localValues.set("message", message);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, hook, "Exception.hx", 40));
	}

	static function aTypedFilterAloneInstallsTheHook(assert:Assert):Void {
		var t = make();
		initialize(t);
		setTypedFilters(t, ["uncaught"], ["SubError"]);
		assert.equals(1, t.api.installedFunctionBreakpoints.length, "typed filters alone arm the hook");
		setTypedFilters(t, ["uncaught"], []);
		assert.equals(1, t.api.deletedBreakpoints.length, "clearing the types disarms it");
	}

	static function aTypedFilterStopsMatchesAndResumesOthers(assert:Assert):Void {
		var t = make();
		initialize(t);
		setTypedFilters(t, [], ["SubError"]); // typed only — "thrown" is OFF
		var hook = t.api.installedFunctionBreakpoints[0].number;

		t.sent.resize(0);
		hookStop(t, hook, new UnrelatedError(), "nope");
		assert.equals(0, t.sent.length, "a non-matching class resumes silently");
		assert.equals(1, t.api.continueCalls.length, "resumed");

		hookStop(t, hook, new SubError("kaboom"), "kaboom");
		assert.equals(1, t.sent.length, "the matching class stops");
		assert.equals("exception", t.sent[0].body.reason, "as an exception stop");
		assert.isTrue(StringTools.endsWith(t.sent[0].body.text, "SubError: kaboom"), "concrete class + message");
	}

	// SubError INHERITS its constructor (no own `new` frame exists), the case
	// a per-type entry breakpoint could never catch: matching walks the class
	// chain read from `this`, so a BaseError filter stops SubError throws.
	static function aBaseClassFilterMatchesSubclassThrows(assert:Assert):Void {
		var t = make();
		initialize(t);
		setTypedFilters(t, [], ["BaseError"]);
		var hook = t.api.installedFunctionBreakpoints[0].number;
		t.sent.resize(0);
		hookStop(t, hook, new SubError("boom"), "boom");
		assert.equals(1, t.sent.length, "the subclass throw stops on the base filter");
		assert.isTrue(StringTools.endsWith(t.sent[0].body.text, "SubError: boom"), "text names the CONCRETE class");
	}

	// The stack reported for a thrown stop must END at the THROW SITE: the
	// exception's own ctor chain (haxe.Exception.new + its subclass ctors) is
	// trimmed, but a USER constructor that itself throws stays visible.
	static function aThrownStopTrimsTheExceptionsOwnCtorFramesOnly(assert:Assert):Void {
		var t = make();
		initialize(t);
		setFilters(t, ["thrown"]);
		var hook = t.api.installedFunctionBreakpoints[0].number;
		t.api.localNames = ["this", "message"];
		t.api.localValues.set("this", new SubError("boom"));
		t.api.localValues.set("message", "boom");
		var subName = Type.getClassName(SubError); // the runtime chain name
		// Widget.new throws: [outermost .. innermost]
		var frames:Array<DebugStackFrame> = [
			new DebugStackFrame("Main.hx", 5, "Main", "main"),
			new DebugStackFrame("Widget.hx", 9, "Widget", "new"), // the throw site — a USER ctor
			new DebugStackFrame("Sub.hx", 3, subName, "new"),
			new DebugStackFrame("Exception.hx", 40, "haxe.Exception", "new")
		];
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(ThreadStopped(1, DebugThread.STATUS_STOPPED_BREAKPOINT, hook, frames, null));
		assert.equals("exception", t.sent[0].body.reason, "reported as a thrown stop");
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({seq: 9, type: "request", command: "stackTrace", arguments: {threadId: 1}}));
		var reported:Array<Dynamic> = t.sent[0].body.stackFrames;
		assert.equals(2, reported.length, "the exception's ctor chain was trimmed");
		assert.equals("Widget.new", reported[0].name, "the top frame is the throw site (a user ctor survives)");
		assert.equals(9, reported[0].line, "at the throw line");
	}

	// The "thrown" filter: a class-function breakpoint on haxe.Exception.new
	// (every subclass constructor runs through it via super()).
	static function theThrownFilterInstallsAndRemovesTheHook(assert:Assert):Void {
		var t = make();
		initialize(t);
		setFilters(t, ["uncaught", "critical", "thrown"]);
		assert.equals(1, t.api.installedFunctionBreakpoints.length, "the hook is installed");
		assert.equals("haxe.Exception", t.api.installedFunctionBreakpoints[0].className, "on haxe.Exception");
		assert.equals("new", t.api.installedFunctionBreakpoints[0].functionName, "at the constructor");
		var hook = t.api.installedFunctionBreakpoints[0].number;
		setFilters(t, ["uncaught", "critical"]);
		assert.isTrue(t.api.deletedBreakpoints.indexOf(hook) >= 0, "disabling removes the hook");
		setFilters(t, ["thrown"]);
		assert.equals(2, t.api.installedFunctionBreakpoints.length, "re-enabling reinstalls");
	}

	static function aThrownHookStopReportsTheExceptionWithItsMessage(assert:Assert):Void {
		var t = make();
		initialize(t);
		setFilters(t, ["thrown"]);
		var hook = t.api.installedFunctionBreakpoints[0].number;
		t.api.localNames = ["this", "message"];
		t.api.localValues.set("message", "kaboom");
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, hook, "Exception.hx", 40));
		assert.equals("exception", t.sent[0].body.reason, "reported as an exception stop");
		assert.equals("Thrown exception", t.sent[0].body.description, "classified as thrown");
		assert.equals("haxe.Exception: kaboom", t.sent[0].body.text, "carries class + message");
		// exceptionInfo reflects the thrown stop
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({seq: 9, type: "request", command: "exceptionInfo", arguments: {threadId: 1}}));
		assert.equals("Thrown exception", t.sent[0].body.exceptionId, "exceptionInfo id");
		assert.equals("always", t.sent[0].body.breakMode, "thrown stops regardless of try/catch");
	}

	static function aMissingExceptionClassMakesTheThrownFilterUnverified(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.api.knownClasses = ["OnlyThisOne"]; // haxe.Exception not compiled in
		t.sent.resize(0);
		setFilters(t, ["thrown"]);
		var results:Array<Dynamic> = t.sent[0].body.breakpoints;
		assert.isTrue(!results[0].verified, "the inert filter reports unverified");
		assert.equals(0, t.api.installedFunctionBreakpoints.length, "nothing installed");
	}

	static function smartStepRequest(t):Void {
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 12));
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({
			seq: 4, type: "request", command: "custom/stepIntoFunction",
			arguments: {threadId: 1, className: "my.pack.Target", functionName: "two"}
		}));
	}

	static function smartStepEntersTheChosenCallee(assert:Assert):Void {
		var t = make();
		initialize(t);
		smartStepRequest(t);
		assert.isTrue(t.sent[0].success, "stepIntoFunction acknowledged");
		assert.equals(1, t.api.installedFunctionBreakpoints.length, "temp entry breakpoint installed");
		assert.equals("my.pack.Target", t.api.installedFunctionBreakpoints[0].className, "on the chosen class");
		assert.equals(StepType.OVER, t.api.stepCalls[0].stepType, "races a step-over");
		var temp = t.api.installedFunctionBreakpoints[0].number;
		// the callee's entry: the temp fires (a breakpoint stop with its number)
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, temp, "Target.hx", 5));
		assert.equals("step", t.sent[0].body.reason, "reported as a plain step stop");
		assert.isTrue(t.sent[0].body.hitBreakpointIds == null, "no breakpoint id leaks to the client");
		assert.isTrue(t.api.deletedBreakpoints.indexOf(temp) >= 0, "the temp died with the stop");
	}

	static function smartStepFallsBackToStepOver(assert:Assert):Void {
		var t = make();
		initialize(t);
		smartStepRequest(t);
		var temp = t.api.installedFunctionBreakpoints[0].number;
		// the chosen call never executed: the step-over lands on the next line
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 13));
		assert.equals("step", t.sent[0].body.reason, "degrades to a plain step over");
		assert.isTrue(t.api.deletedBreakpoints.indexOf(temp) >= 0, "the unfired temp died with the stop");
	}

	static function aUserBreakpointWinsTheSmartStepRace(assert:Assert):Void {
		var t = make();
		initialize(t);
		var user = conditionalBreakpoint(t, ""); // a plain user breakpoint
		smartStepRequest(t);
		var temp = t.api.installedFunctionBreakpoints[0].number;
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, user, "Main.hx", 20));
		assert.equals("breakpoint", t.sent[0].body.reason, "the user breakpoint reports normally");
		assert.isTrue(t.api.deletedBreakpoints.indexOf(temp) >= 0, "the temp still died with the stop");
	}

	// Real debuggees fault their readers (raw pointers, half-built state in
	// frames like a thread pool's dispatch loop). One corrupt slot must render
	// as an error row; the request — and the session — must keep working.
	static function aCorruptLocalPoisonsOneRowNotTheRequest(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.api.localNames = ["ok", "bad"];
		t.api.localValues.set("ok", 5);
		t.api.corruptLocals = ["bad"];
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 9));
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({seq: 5, type: "request", command: "stackTrace", arguments: {threadId: 1}}));
		var frameId = t.sent[0].body.stackFrames[0].id;
		t.dispatcher.handleRequest(Json.stringify({seq: 6, type: "request", command: "scopes", arguments: {frameId: frameId}}));
		var reference = t.sent[1].body.scopes[0].variablesReference;
		t.dispatcher.handleRequest(Json.stringify({seq: 7, type: "request", command: "variables", arguments: {variablesReference: reference}}));
		var response = t.sent[2];
		assert.isTrue(response.success, "variables succeeds despite the corrupt slot");
		var rows:Array<Dynamic> = response.body.variables;
		assert.equals(2, rows.length, "both locals listed");
		assert.equals("5", rows[0].value, "healthy local rendered");
		assert.isTrue(StringTools.startsWith(rows[1].value, "<unreadable"), "corrupt local rendered as an error row");
	}

	static function aFaultingHandlerAnswersAndTheSessionLivesOn(assert:Assert):Void {
		var t = make();
		initialize(t);
		// no stop happened, so this threads() call is fine — instead fault the
		// handler itself: threads() over a null canned list throws inside dispatch
		t.api.cannedThreads = null;
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({seq: 8, type: "request", command: "threads"}));
		assert.isTrue(!t.sent[0].success, "the faulting request is answered with an error");
		// and the session still serves the next request
		t.api.cannedThreads = [];
		t.dispatcher.handleRequest(Json.stringify({seq: 9, type: "request", command: "threads"}));
		assert.isTrue(t.sent[1].success, "the session lives on after the fault");
	}

	// Resuming a critical error re-faults on the spot (observed live: null
	// deref -> fixup -> segv -> stop again), so with the filter off the silent
	// resumes are capped and the stop is then reported anyway.
	static function repeatedSilentCriticalResumesBreakTheLivelock(assert:Assert):Void {
		var t = make();
		initialize(t);
		setFilters(t, ["uncaught"]); // critical OFF
		t.sent.resize(0);
		for (_ in 0...3) {
			t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_CRITICAL_ERROR, -1,
				"Main.hx", 43, "Null Object Reference"));
		}
		assert.equals(0, t.sent.length, "first three re-faults resume silently");
		assert.equals(3, t.api.continueCalls.length, "three silent resumes");
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_CRITICAL_ERROR, -1,
			"Main.hx", 43, "Null Object Reference"));
		assert.equals(1, t.sent.length, "the fourth is reported despite the filter");
		assert.equals("exception", t.sent[0].body.reason, "reported as an exception stop");
		assert.equals(3, t.api.continueCalls.length, "no further silent resume");
	}

	static function setFilters(t, filters:Array<String>):Void {
		t.dispatcher.handleRequest(Json.stringify({
			seq: 2, type: "request", command: "setExceptionBreakpoints",
			arguments: {filters: filters}
		}));
	}

	static function anUncaughtThrowStopsAsException(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_CRITICAL_ERROR, -1,
			"Main.hx", 16, "Uncatchable Throw: boom"));
		assert.equals(1, t.sent.length, "one stopped event");
		assert.equals("exception", t.sent[0].body.reason, "reason is exception");
		assert.equals("Uncaught exception", t.sent[0].body.description, "classified as uncaught");
		assert.equals("Uncatchable Throw: boom", t.sent[0].body.text, "carries the runtime message");
	}

	static function aCriticalErrorStopsAsException(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_CRITICAL_ERROR, -1,
			"Main.hx", 16, "Null Object Reference"));
		assert.equals("exception", t.sent[0].body.reason, "reason is exception");
		assert.equals("Critical error", t.sent[0].body.description, "classified as critical");
	}

	static function aDisabledFilterResumesSilently(assert:Assert):Void {
		var t = make();
		initialize(t);
		setFilters(t, ["critical"]); // uncaught OFF
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_CRITICAL_ERROR, -1,
			"Main.hx", 16, "Uncatchable Throw: boom"));
		assert.equals(0, t.sent.length, "no stopped event for a disabled filter");
		assert.equals(1, t.api.continueCalls.length, "resumed silently");
		// the OTHER kind still stops
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_CRITICAL_ERROR, -1,
			"Main.hx", 16, "Null Object Reference"));
		assert.equals(1, t.sent.length, "critical still enabled");
	}

	static function exceptionInfoDescribesTheLastStop(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_CRITICAL_ERROR, -1,
			"Main.hx", 16, "Uncatchable Throw: boom"));
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({seq: 3, type: "request", command: "exceptionInfo", arguments: {threadId: 1}}));
		assert.isTrue(t.sent[0].success, "exceptionInfo succeeds");
		assert.equals("Uncatchable Throw: boom", t.sent[0].body.description, "returns the runtime message");
		assert.equals("unhandled", t.sent[0].body.breakMode, "uncaught maps to unhandled");
	}

	static function exceptionInfoFailsWhenNotAtAnExceptionStop(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 9));
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({seq: 3, type: "request", command: "exceptionInfo", arguments: {threadId: 1}}));
		assert.isTrue(!t.sent[0].success, "exceptionInfo fails at a non-exception stop");
	}

	static function evaluateReturnsAResult(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, -1, "Main.hx", 10));
		t.api.localNames = ["count"];
		t.api.localValues.set("count", 6);
		t.sent.resize(0);
		t.dispatcher.handleRequest(Json.stringify({
			seq: 1, type: "request", command: "evaluate",
			arguments: {expression: "count * 7", frameId: 0}
		}));
		assert.isTrue(t.sent[0].success, "evaluate succeeds");
		assert.equals("42", t.sent[0].body.result, "expression evaluated against the frame");
	}

	static function conditionalBreakpoint(t, condition:String):Int {
		t.api.cannedFilesFullPath = ["C:/src/Main.hx"];
		t.api.cannedFiles = ["Main.hx"];
		t.dispatcher.handleRequest(Json.stringify({
			seq: 1, type: "request", command: "setBreakpoints",
			arguments: {source: {path: "C:/src/Main.hx"}, breakpoints: [{line: 20, condition: condition}]}
		}));
		return t.api.installedBreakpoints[0].number;
	}

	static function aFalseConditionResumesWithoutStopping(assert:Assert):Void {
		var t = make();
		initialize(t);
		var rt = conditionalBreakpoint(t, "count > 5");
		t.api.localNames = ["count"];
		t.api.localValues.set("count", 3); // condition false
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, rt, "Main.hx", 20));
		assert.equals(0, t.sent.length, "no stopped event when the condition is false");
		assert.equals(1, t.api.continueCalls.length, "resumed silently");
	}

	static function aTrueConditionStops(assert:Assert):Void {
		var t = make();
		initialize(t);
		var rt = conditionalBreakpoint(t, "count > 5");
		t.api.localNames = ["count"];
		t.api.localValues.set("count", 10); // condition true
		t.sent.resize(0);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, rt, "Main.hx", 20));
		assert.equals(1, t.sent.length, "stopped when the condition is true");
		assert.equals("breakpoint", t.sent[0].body.reason, "reported as a breakpoint");
	}

	// A ThreadStopped event with an optional single-frame stack.
	static function stop(number:Int, status:Int, breakpoint:Int = -1, ?file:String, ?line:Int, ?description:String):DebugEvent {
		var stack:Array<DebugStackFrame> = [];
		if (file != null) {
			stack.push(new DebugStackFrame(file, line != null ? line : 0, "Main", "fn"));
		}
		return ThreadStopped(number, status, breakpoint, stack, description);
	}

	static function pauseBreaksTheWorld(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.dispatcher.handleRequest(request("pause", 3));
		assert.isTrue(t.sent[0].success, "pause acked");
		assert.equals(1, t.api.breakNowCalls, "breakNow called");
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 5));
		assert.equals("pause", t.sent[1].body.reason, "an immediate break with no step in flight is a pause");
	}

	static function stepIssuesTheRightStepTypeAndReportsStep(assert:Assert):Void {
		var t = make();
		initialize(t);
		// prime a current stop so the step has a "from" line
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, -1, "Main.hx", 10));
		t.sent.resize(0);
		t.dispatcher.handleRequest(stepRequest("next", 3, 1));
		assert.equals(1, t.api.stepCalls.length, "stepThread issued");
		assert.equals(2, t.api.stepCalls[0].stepType, "next -> STEP_OVER");
		// landing on a different line -> stop with reason step
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 11));
		assert.equals("step", t.sent[1].body.reason, "step landing reported as step");
	}

	static function aStepThatKeepsTheSameLineReSteps(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, -1, "Main.hx", 10));
		t.sent.resize(0);
		t.dispatcher.handleRequest(stepRequest("next", 3, 1));
		// still on line 10 (multi-expression line) -> re-step, no stopped event
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 10));
		assert.equals(2, t.api.stepCalls.length, "re-stepped on the same line");
		assert.equals(1, t.sent.length, "no stopped event while still on the same line (just the step response)");
		// now the line changes -> report
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE, -1, "Main.hx", 12));
		assert.equals("step", t.sent[1].body.reason, "reported once the line changed");
	}

	static function aBreakpointHitMidStepWinsOverTheStep(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.api.cannedFilesFullPath = ["C:/src/Main.hx"];
		t.api.cannedFiles = ["Main.hx"];
		t.dispatcher.handleRequest(Json.stringify({
			seq: 1, type: "request", command: "setBreakpoints",
			arguments: {source: {path: "C:/src/Main.hx"}, breakpoints: [{line: 20}]}
		}));
		var runtimeNumber = t.api.installedBreakpoints[0].number;
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, -1, "Main.hx", 10));
		t.sent.resize(0);
		t.dispatcher.handleRequest(stepRequest("next", 3, 1));
		// the step ran into a breakpoint: report breakpoint, not step
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, runtimeNumber, "Main.hx", 20));
		assert.equals("breakpoint", t.sent[1].body.reason, "a breakpoint mid-step wins");
	}

	static function stackTraceReportsFramesNewestFirst(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.api.cannedFiles = ["Main.hx"];
		t.api.cannedFilesFullPath = ["C:/src/Main.hx"];
		// hxcpp orders innermost LAST: main() then add()
		var stack = [
			new DebugStackFrame("Main.hx", 11, "Main", "main"),
			new DebugStackFrame("Main.hx", 17, "Main", "add")
		];
		t.dispatcher.handleDebugEvent(ThreadStopped(1, DebugThread.STATUS_STOPPED_BREAKPOINT, -1, stack, null));
		t.sent.resize(0);
		t.dispatcher.handleRequest(stepRequest("stackTrace", 5, 1));
		var frames = t.sent[0].body.stackFrames;
		assert.equals(2, frames.length, "two frames");
		assert.equals("Main.add", frames[0].name, "newest (innermost) frame first");
		assert.equals(17, frames[0].line, "innermost line");
		assert.equals("C:/src/Main.hx", frames[0].source.path, "short file mapped to full path");
		assert.equals("Main.main", frames[1].name, "caller second");
	}

	static function stepRequest(command:String, seq:Int, threadId:Int):String {
		return Json.stringify({seq: seq, type: "request", command: command, arguments: {threadId: threadId}});
	}

	static function eventsBeforeInitializeAreBufferedThenFlushed(assert:Assert):Void {
		var t = make();
		// a debuggee whose main thread already exists fires this immediately
		t.dispatcher.handleDebugEvent(ThreadCreated(1));
		assert.equals(0, t.sent.length, "nothing emitted before initialize");
		t.dispatcher.handleRequest(request("initialize", 1));
		// response, initialized event, THEN the buffered thread event
		assert.equals("thread", t.sent[2].event, "buffered event flushed after initialized");
		assert.equals("started", t.sent[2].body.reason, "the buffered thread-created event");
	}

	static function continueResumesAllThreads(assert:Assert):Void {
		var t = make();
		t.dispatcher.handleRequest(request("continue", 3));
		assert.isTrue(t.sent[0].success, "continue acked");
		assert.equals(1, t.api.continueCalls.length, "runtime resumed");
		assert.equals(-1, t.api.continueCalls[0].threadNumber, "all threads (-1)");
	}

	static function make():{dispatcher:Dispatcher, api:FakeDebuggerApi, sent:Array<Dynamic>} {
		var sent:Array<Dynamic> = [];
		var api = new FakeDebuggerApi();
		var dispatcher = new Dispatcher(api, payload -> sent.push(Json.parse(payload)));
		return {dispatcher: dispatcher, api: api, sent: sent};
	}

	static function request(command:String, seq:Int):String {
		return Json.stringify({seq: seq, type: "request", command: command});
	}

	static function initializeRespondsThenEmitsInitialized(assert:Assert):Void {
		var t = make();
		t.dispatcher.handleRequest(request("initialize", 1));
		assert.equals(2, t.sent.length, "response plus initialized event");
		assert.equals("response", t.sent[0].type, "response first");
		assert.isTrue(t.sent[0].success, "initialize succeeds");
		assert.equals(1, t.sent[0].request_seq, "request seq echoed");
		assert.isTrue(t.sent[0].body.supportsConfigurationDoneRequest, "capabilities present");
		assert.equals("initialized", t.sent[1].event, "initialized event strictly after");
	}

	static function threadsComeFromTheDebuggerApi(assert:Assert):Void {
		var t = make();
		t.api.cannedThreads = [
			new DebugThread(1, DebugThread.STATUS_STOPPED_BREAKPOINT, 3),
			new DebugThread(5, DebugThread.STATUS_RUNNING)
		];
		t.dispatcher.handleRequest(request("threads", 7));
		var body = t.sent[0].body;
		assert.equals(2, body.threads.length, "both threads listed");
		assert.equals(5, body.threads[1].id, "thread numbers are the DAP ids");
	}

	static function disconnectAcksAndRequestsShutdown(assert:Assert):Void {
		var t = make();
		t.dispatcher.handleRequest(request("disconnect", 9));
		assert.isTrue(t.sent[0].success, "disconnect acked");
		assert.isTrue(t.dispatcher.shutdownRequested, "shutdown requested");
	}

	static function unknownCommandFailsWithoutKillingTheSession(assert:Assert):Void {
		var t = make();
		t.dispatcher.handleRequest(request("fancyNewThing", 3));
		assert.isTrue(t.sent[0].success == false, "unknown command fails");
		t.dispatcher.handleRequest(request("threads", 4));
		assert.isTrue(t.sent[1].success, "session still serves requests");
	}

	static function invalidJsonFailsGracefully(assert:Assert):Void {
		var t = make();
		t.dispatcher.handleRequest("{not json");
		assert.isTrue(t.sent[0].success == false, "invalid JSON gets a failure response");
	}

	// Sends initialize and drops the response+initialized event, so a test can
	// assert on the events that follow (nothing is emitted before initialize).
	static function initialize(t:{dispatcher:Dispatcher, api:FakeDebuggerApi, sent:Array<Dynamic>}):Void {
		t.dispatcher.handleRequest(request("initialize", 1));
		t.sent.resize(0);
	}

	static function runtimeEventsMapToDapEvents(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.dispatcher.handleDebugEvent(stop(2, DebugThread.STATUS_STOPPED_BREAKPOINT));
		t.dispatcher.handleDebugEvent(stop(2, DebugThread.STATUS_STOPPED_UNCAUGHT_EXCEPTION));
		t.dispatcher.handleDebugEvent(stop(2, DebugThread.STATUS_STOPPED_BREAK_IMMEDIATE));
		t.dispatcher.handleDebugEvent(ThreadCreated(4));
		t.dispatcher.handleDebugEvent(ThreadTerminated(4));
		assert.equals("breakpoint", t.sent[0].body.reason, "breakpoint stop reason");
		assert.equals("exception", t.sent[1].body.reason, "exception stop reason");
		assert.equals("pause", t.sent[2].body.reason, "immediate-break stop reason");
		assert.equals("started", t.sent[3].body.reason, "thread created");
		assert.equals("exited", t.sent[4].body.reason, "thread terminated");
		assert.isTrue(t.sent[0].body.allThreadsStopped, "hxcpp stops the world");
	}

	static function setBreakpointsInstallsAndReturnsResults(assert:Assert):Void {
		var t = make();
		t.api.cannedFilesFullPath = ["C:/build/src/Main.hx"];
		t.api.cannedFiles = ["Main.hx"];
		var payload = Json.stringify({
			seq: 1, type: "request", command: "setBreakpoints",
			arguments: {source: {path: "C:/build/src/Main.hx"}, breakpoints: [{line: 10}, {line: 12}]}
		});
		t.dispatcher.handleRequest(payload);
		assert.isTrue(t.sent[0].success, "setBreakpoints succeeds");
		assert.equals(2, t.sent[0].body.breakpoints.length, "one result per requested line");
		assert.isTrue(t.sent[0].body.breakpoints[0].verified, "verified against the matched file");
		assert.equals(2, t.api.installedBreakpoints.length, "installed in the runtime");
	}

	static function aBreakpointStopCarriesTheHitId(assert:Assert):Void {
		var t = make();
		initialize(t);
		t.api.cannedFilesFullPath = ["C:/build/src/Main.hx"];
		t.api.cannedFiles = ["Main.hx"];
		t.dispatcher.handleRequest(Json.stringify({
			seq: 1, type: "request", command: "setBreakpoints",
			arguments: {source: {path: "C:/build/src/Main.hx"}, breakpoints: [{line: 10}]}
		}));
		var runtimeNumber = t.api.installedBreakpoints[0].number;
		var dapId = t.sent[0].body.breakpoints[0].id;
		t.dispatcher.handleDebugEvent(stop(1, DebugThread.STATUS_STOPPED_BREAKPOINT, runtimeNumber));
		var stopped = t.sent[1];
		assert.equals("breakpoint", stopped.body.reason, "breakpoint stop");
		assert.equals(dapId, stopped.body.hitBreakpointIds[0], "the hit id maps back to the DAP breakpoint");
	}
}

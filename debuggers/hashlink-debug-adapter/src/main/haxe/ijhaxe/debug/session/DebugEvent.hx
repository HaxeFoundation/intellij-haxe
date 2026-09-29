package ijhaxe.debug.session;
import ijhaxe.debug.DebugErrorCode;
import ijhaxe.debug.breakpoints.BreakpointResult;
import ijhaxe.debug.target.ThreadInfo;
import ijhaxe.debug.inspect.ScopeInfo;

import ijhaxe.debug.values.VariableInfo;

/**
	An event from the DebugSession thread. The dispatcher on the worker thread
	turns it into DAP responses or events.
**/
enum DebugEvent {
	// completions of deferred responses, carrying the request seq
	EvLaunched(requestSeq:Int);
	EvLaunchFailed(requestSeq:Int, message:String);
	EvBreakpoints(requestSeq:Int, results:Array<BreakpointResult>);
	EvConfigurationDone(requestSeq:Int);
	EvContinued(requestSeq:Int);

	EvStepStarted(requestSeq:Int); // ack for a next/stepIn/stepOut request; the stop follows
	EvPaused(requestSeq:Int); // ack for a pause request; the stopped(reason:"pause") event follows
	EvExceptionBreakpointsSet(requestSeq:Int); // ack for a setExceptionBreakpoints request
	EvToStringRenderingSet(requestSeq:Int); // ack for a custom/setToStringRendering request

	EvThreads(requestSeq:Int, threads:Array<ThreadInfo>);
	EvStepInTargets(requestSeq:Int, targets:Array<StepInTargetInfo>);
	EvStackTrace(requestSeq:Int, frames:Array<FrameInfo>);
	EvScopes(requestSeq:Int, scopes:Array<ScopeInfo>);
	EvVariables(requestSeq:Int, variables:Array<VariableInfo>);
	EvVariableSet(requestSeq:Int, result:VariableInfo);
	EvEvaluated(requestSeq:Int, result:VariableInfo);

	// `code` and `variables` become the DAP Message.id and Message.variables, so
	// the client can branch on a stable code (UnresolvedName, for example)
	// instead of the message text.
	EvRejected(requestSeq:Int, message:String, code:DebugErrorCode, variables:Null<Map<String, String>>);
	EvSessionEnded(requestSeq:Int);

	// spontaneous events
	EvBreakpointChanged(result:BreakpointResult);
	EvStoppedBreakpoint(threadId:Int, hitBreakpointIds:Array<Int>);
	EvStoppedStep(threadId:Int);
	EvStoppedException(threadId:Int, description:String);
	EvStoppedPause(threadId:Int); // the debuggee was interrupted by a user pause

	// A step found no landing in user code (stepping past the last statement of
	// a thread's entry function, for example) and the debuggee runs on. Tells the
	// client it is running, so it stops waiting for a step stop that cannot come.
	EvResumed(threadId:Int);

	EvOutput(category:String, text:String);
	EvExited(exitCode:Int);
}

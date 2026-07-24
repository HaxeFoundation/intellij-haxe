package ijhaxe.debug.session;
import ijhaxe.debug.DebugErrorCode;
import ijhaxe.debug.breakpoints.BreakpointResult;
import ijhaxe.debug.target.ThreadInfo;
import ijhaxe.debug.inspect.ScopeInfo;

import ijhaxe.debug.values.VariableInfo;

/**
	An event emitted by the DebugSession thread, consumed by the worker/dispatcher
	and turned into DAP responses or events.
**/
enum DebugEvent {
	// deferred-response completions (carry the originating request seq)
	EvLaunched(requestSeq:Int);
	EvLaunchFailed(requestSeq:Int, message:String);
	EvBreakpoints(requestSeq:Int, results:Array<BreakpointResult>);
	EvConfigurationDone(requestSeq:Int);
	EvContinued(requestSeq:Int);
	EvStepStarted(requestSeq:Int); // ack for a next/stepIn/stepOut request; the stop follows
	EvPaused(requestSeq:Int); // ack for a pause request; the stopped(reason:"pause") event follows
	EvExceptionBreakpointsSet(requestSeq:Int); // ack for a setExceptionBreakpoints request
	EvToStringRenderingSet(requestSeq:Int); // ack for an custom/setToStringRendering request
	EvThreads(requestSeq:Int, threads:Array<ThreadInfo>);
	EvStepInTargets(requestSeq:Int, targets:Array<StepInTargetInfo>);
	EvStackTrace(requestSeq:Int, frames:Array<FrameInfo>);
	EvScopes(requestSeq:Int, scopes:Array<ScopeInfo>);
	EvVariables(requestSeq:Int, variables:Array<VariableInfo>);
	EvVariableSet(requestSeq:Int, result:VariableInfo);
	EvEvaluated(requestSeq:Int, result:VariableInfo);
	// `code`/`variables` carry the DAP Message.id + Message.variables so the client
	// can branch on a stable code (e.g. UnresolvedName) rather than the message text.
	EvRejected(requestSeq:Int, message:String, code:DebugErrorCode, variables:Null<Map<String, String>>);
	EvSessionEnded(requestSeq:Int);
	// spontaneous events
	EvBreakpointChanged(result:BreakpointResult);
	EvStoppedBreakpoint(threadId:Int, hitBreakpointIds:Array<Int>);
	EvStoppedStep(threadId:Int);
	EvStoppedException(threadId:Int, description:String);
	EvStoppedPause(threadId:Int); // the debuggee was interrupted by a user pause
	// The debuggee resumed on its own after a step that had no user-code landing
	// (e.g. stepping past a thread entry's last statement): tell the client it is
	// running so it stops waiting for a step stop that can never arrive.
	EvResumed(threadId:Int);
	EvOutput(category:String, text:String);
	EvExited(exitCode:Int);
}

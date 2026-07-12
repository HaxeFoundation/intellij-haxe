package debug.session;
import debug.values.ScopeInfo;

import debug.values.VariableInfo;

/**
 * An event emitted by the DebugSession thread, consumed by the worker/dispatcher
 * and turned into DAP responses or events.
 */
enum DebugEvent {
	// deferred-response completions (carry the originating request seq)
	EvLaunched(requestSeq:Int);
	EvLaunchFailed(requestSeq:Int, message:String);
	EvBreakpoints(requestSeq:Int, results:Array<BreakpointResult>);
	EvConfigurationDone(requestSeq:Int);
	EvContinued(requestSeq:Int);
	EvStepStarted(requestSeq:Int); // ack for a next/stepIn/stepOut request; the stop follows
	EvStackTrace(requestSeq:Int, frames:Array<FrameInfo>);
	EvScopes(requestSeq:Int, scopes:Array<ScopeInfo>);
	EvVariables(requestSeq:Int, variables:Array<VariableInfo>);
	EvVariableSet(requestSeq:Int, result:VariableInfo);
	EvEvaluated(requestSeq:Int, result:VariableInfo);
	EvRejected(requestSeq:Int, message:String);
	EvSessionEnded(requestSeq:Int);
	// spontaneous events
	EvBreakpointChanged(result:BreakpointResult);
	EvStoppedBreakpoint(threadId:Int, hitBreakpointIds:Array<Int>);
	EvStoppedStep(threadId:Int);
	EvStoppedException(threadId:Int, description:String);
	EvOutput(category:String, text:String);
	EvExited(exitCode:Int);
}

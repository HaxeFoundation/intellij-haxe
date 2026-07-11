package debug;

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
	EvStackTrace(requestSeq:Int, frames:Array<FrameInfo>);
	EvRejected(requestSeq:Int, message:String);
	EvSessionEnded(requestSeq:Int);
	// spontaneous events
	EvBreakpointChanged(result:BreakpointResult);
	EvStoppedBreakpoint(threadId:Int, hitBreakpointIds:Array<Int>);
	EvStoppedException(threadId:Int, description:String);
	EvOutput(category:String, text:String);
	EvExited(exitCode:Int);
}

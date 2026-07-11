package debug;

/**
 * A command handed to the DebugSession thread. `requestSeq` echoes the DAP
 * request that triggered it so the completion event can be turned back into
 * the right response (deferred responses).
 */
enum SessionCommand {
	CmdLaunch(requestSeq:Int, config:LaunchConfig);
	// isReverify = a breakpoint set that was already answered (pre-launch); the
	// session emits BreakpointChanged events instead of a setBreakpoints response.
	CmdSetBreakpoints(requestSeq:Int, sourceKey:String, sourcePath:String, breakpoints:Array<RequestedBreakpoint>, isReverify:Bool);
	CmdConfigurationDone(requestSeq:Int);
	CmdContinue(requestSeq:Int, threadId:Int);
	CmdStackTrace(requestSeq:Int, threadId:Int);
	CmdDisconnect(requestSeq:Int);
}

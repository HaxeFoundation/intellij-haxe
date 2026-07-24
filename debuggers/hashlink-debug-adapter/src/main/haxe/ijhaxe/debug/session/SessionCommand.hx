package ijhaxe.debug.session;

import ijhaxe.debug.breakpoints.RequestedBreakpoint;

/**
	A command handed to the DebugSession thread. `requestSeq` echoes the DAP
	request that triggered it so the completion event can be turned back into
	the right response (deferred responses).
**/
enum SessionCommand {
	CmdLaunch(requestSeq:Int, config:LaunchConfig);
	// isReverify = a breakpoint set that was already answered (pre-launch); the
	// session emits BreakpointChanged events instead of a setBreakpoints response.
	CmdSetBreakpoints(requestSeq:Int, sourceKey:String, sourcePath:String, breakpoints:Array<RequestedBreakpoint>, isReverify:Bool);
	CmdConfigurationDone(requestSeq:Int);
	CmdContinue(requestSeq:Int, threadId:Int);
	// `targetId` (stepIn only): the call-opcode id from a preceding stepInTargets
	// — enter THAT call instead of the first one reached; null = plain step.
	CmdStep(requestSeq:Int, threadId:Int, mode:StepMode, targetId:Null<Int>);
	// Lists the calls on `frameId`'s stopped line as step-into choices.
	CmdStepInTargets(requestSeq:Int, frameId:Int);
	CmdPause(requestSeq:Int, threadId:Int);
	// Enables/disables breaking on thrown exceptions; `filters` is the DAP filter
	// id list (non-empty = enable "all exceptions", empty = disable).
	CmdSetExceptionBreakpoints(requestSeq:Int, filters:Array<String>, filterTypes:Array<String>);
	CmdThreads(requestSeq:Int);
	CmdStackTrace(requestSeq:Int, threadId:Int);
	CmdScopes(requestSeq:Int, frameId:Int);
	CmdVariables(requestSeq:Int, reference:Int);
	CmdSetVariable(requestSeq:Int, reference:Int, name:String, value:String);
	CmdEvaluate(requestSeq:Int, frameId:Int, expression:String);
	// The user's opt-in for toString object labels (custom
	// custom/setToStringRendering request), toggleable live.
	CmdSetToStringRendering(requestSeq:Int, enabled:Bool);
	CmdDisconnect(requestSeq:Int);
}

package ijhaxe.debug.session;

import ijhaxe.debug.breakpoints.RequestedBreakpoint;

/**
	A command for the DebugSession thread. `requestSeq` is the seq of the DAP
	request behind it, so the completion event can answer that request with a
	deferred response.
**/
enum SessionCommand {
	CmdLaunch(requestSeq:Int, config:LaunchConfig);
	// `isReverify`: the request was already answered provisionally before
	// launch, so the session emits EvBreakpointChanged events instead of a
	// setBreakpoints response.
	CmdSetBreakpoints(requestSeq:Int, sourceKey:String, sourcePath:String, breakpoints:Array<RequestedBreakpoint>, isReverify:Bool);
	CmdConfigurationDone(requestSeq:Int);

	CmdContinue(requestSeq:Int, threadId:Int);
	// `targetId` (stepIn only): the opcode id of a call listed by stepInTargets,
	// which the step enters; null for a plain step.
	CmdStep(requestSeq:Int, threadId:Int, mode:StepMode, targetId:Null<Int>);
	// Lists the calls on the stopped line of `frameId` as step-into choices.
	CmdStepInTargets(requestSeq:Int, frameId:Int);
	CmdPause(requestSeq:Int, threadId:Int);

	// Sets the exception breakpoints: `filters` holds the active filter ids
	// ("all", "uncaught", "vm"), `filterTypes` the exception class names to stop on.
	CmdSetExceptionBreakpoints(requestSeq:Int, filters:Array<String>, filterTypes:Array<String>);

	CmdThreads(requestSeq:Int);
	CmdStackTrace(requestSeq:Int, threadId:Int);
	CmdScopes(requestSeq:Int, frameId:Int);
	CmdVariables(requestSeq:Int, reference:Int);
	CmdSetVariable(requestSeq:Int, reference:Int, name:String, value:String);
	CmdEvaluate(requestSeq:Int, frameId:Int, expression:String);

	// The user's live opt-in for toString object labels (the custom
	// custom/setToStringRendering request).
	CmdSetToStringRendering(requestSeq:Int, enabled:Bool);

	CmdDisconnect(requestSeq:Int);
}

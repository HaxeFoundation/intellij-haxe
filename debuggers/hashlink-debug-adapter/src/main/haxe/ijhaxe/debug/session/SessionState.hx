package ijhaxe.debug.session;

/**
	The debug session's run state. Owned by DebugSession; the controllers
	(stepping, line breakpoints, exceptions) read it for guards and set the
	Running/Stopped transitions their operations cause.
**/
enum SessionState {
	NotStarted;
	Configured; // attached, breakpoints patchable, debuggee still held on the handshake socket
	Running;
	Stopped(threadId:Int);
	Exited;
}

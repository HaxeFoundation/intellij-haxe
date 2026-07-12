package debug.inspect;

import debug.target.StackFrameLocation;

/** A walked stack frame plus the globally-unique id the client refers to it by. */
typedef CachedFrame = {
	var frameId:Int;
	var threadId:Int;
	var index:Int; // position in its thread's stack (0 = top)
	var location:StackFrameLocation;
}

package debug;

/**
 * One resolved stack frame as bytecode coordinates plus its machine address.
 * Source file/line/name are attached later by the caller via ModuleDebugInfo.
 */
typedef StackFrameLocation = {
	var fidx:Int;
	var op:Int;
	var address:Pointer;
}

package debug.target;
import dap.protocol.Source;

import debug.Pointer;
import debug.module.ModuleDebugInfo;

/**
 * One resolved stack frame as bytecode coordinates plus its machine address.
 * Source file/line/name are attached later by the caller via ModuleDebugInfo.
 */
typedef StackFrameLocation = {
	var fidx:Int;
	var op:Int;
	var address:Pointer;
	// the frame base (ebp/rbp) of this frame, used to read its locals/arguments
	var ebp:Pointer;
}

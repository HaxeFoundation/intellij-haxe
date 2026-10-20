package ijhaxe.debug.module;

/**
	Where a step plants temporary breakpoints, found by walking a function's
	control-flow graph from the current opcode:
	 - `lineChangeOps`: the first opcode of every reachable source line other
	   than the current one, where step over and step in land in this frame.
	 - `callOps`: the call opcodes reached before the line changes, the
	   candidates for step in. The caller resolves their entry addresses.
	 - `returns`: whether the function can end before the line changes, in
	   which case the step also stops at the caller's return address.
**/
typedef StepTargets = {
	var lineChangeOps:Array<Int>;
	var callOps:Array<Int>;
	var returns:Bool;
}

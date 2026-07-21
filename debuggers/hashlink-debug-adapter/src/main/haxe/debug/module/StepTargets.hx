package debug.module;

/**
	Opcode positions a step should plant temporary breakpoints on, computed by
	walking a function's control-flow graph from the current opcode:
	 - `lineChangeOps`: the first opcode of every reachable source line other than
	   the current one (where "step over" and "step in" land within the frame);
	 - `callOps`: call opcodes reached before a line change (candidate "step in"
	   targets — the callee entry addresses are resolved by the caller);
	 - `returns`: true when a return/throw is reachable, so a step should also stop
	   at the caller's return address.
**/
typedef StepTargets = {
	var lineChangeOps:Array<Int>;
	var callOps:Array<Int>;
	var returns:Bool;
}

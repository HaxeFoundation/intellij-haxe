package ijhaxe.debug.session;

/**
	One selectable "step into" target on the stopped line (DAP StepInTarget):
	a call the step can enter. `id` is the call's opcode index in the stopped
	function — stateless, so a later stepIn with this `targetId` re-resolves the
	callee from the same opcode. `label` is the callee's qualified name.
**/
typedef StepInTargetInfo = {
	var id:Int;
	var label:String;
}

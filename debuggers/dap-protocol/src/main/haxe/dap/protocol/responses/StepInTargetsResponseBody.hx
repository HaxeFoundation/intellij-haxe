package dap.protocol.responses;

/**
	Body of a "stepInTargets" response: the calls on the stopped line the user
	can choose to step into (execution order). A following "stepIn" request may
	carry one target's `id` as its `targetId`.
**/
typedef StepInTargetsResponseBody = {
	var targets:Array<{id:Int, label:String}>;
}

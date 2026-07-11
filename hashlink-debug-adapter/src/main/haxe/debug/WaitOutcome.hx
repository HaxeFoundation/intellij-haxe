package debug;

/**
 * Result of DebugApi.wait: what happened and which thread it happened on.
 */
typedef WaitOutcome = {
	var result:WaitResult;
	var threadId:Int;
}

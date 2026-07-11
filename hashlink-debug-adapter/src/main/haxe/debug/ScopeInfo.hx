package debug;

/**
 * A variable scope (e.g. "Locals") the session hands back for a frame.
 */
typedef ScopeInfo = {
	var name:String;
	var reference:Int;
}

package ijhaxe.debug.inspect;

/**
	A variable scope (e.g. "Locals") the session hands back for a frame.
**/
typedef ScopeInfo = {
	var name:String;
	var reference:Int;
	// DAP Scope.presentationHint ("locals", "registers", ...), when meaningful
	var ?hint:String;
}

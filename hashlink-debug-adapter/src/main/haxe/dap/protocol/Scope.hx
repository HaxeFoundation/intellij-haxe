package dap.protocol;

/**
 * A named container of variables (e.g. "Locals") returned by "scopes".
 * `variablesReference` is the handle the client passes to "variables".
 */
typedef Scope = {
	var name:String;
	var variablesReference:Int;
	@:optional var expensive:Bool;
	@:optional var presentationHint:String;
}

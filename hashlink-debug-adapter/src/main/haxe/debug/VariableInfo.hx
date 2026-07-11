package debug;

/**
 * A resolved variable the session hands back: name plus its decoded value.
 */
typedef VariableInfo = {
	var name:String;
	var value:String;
	var type:String;
	var reference:Int;
}

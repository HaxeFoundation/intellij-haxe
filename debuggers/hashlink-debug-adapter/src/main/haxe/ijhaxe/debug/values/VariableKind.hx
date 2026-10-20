package ijhaxe.debug.values;

/**
	What kind of variable a value is, so the client can pick its icon. Each
	value is the string sent in the DAP `Variable.kind` field, and `to String`
	makes it encode to JSON as that plain string.
**/
enum abstract VariableKind(String) to String {
	// No particular kind: the client shows the plain value icon. An absent or
	// unknown wire kind also decodes to this, so neither side needs a null.
	var Unspecified = "unspecified";

	var Argument = "argument";
	var Local = "local";
	var Static = "static";
	var Field = "field";
}

package debug.values;

/**
	How a variable is classified, so the client can pick a node icon. The
	underlying value IS the wire string carried in the DAP `Variable.kind` field;
	`to String` keeps JSON encoding a plain string, so the adapter stays enum-typed
	without changing the protocol.
**/
enum abstract VariableKind(String) to String {
	// No special classification — the client shows the plain value icon. Also the
	// decoded value for an absent or unrecognized wire kind, so neither side needs
	// a null.
	var Unspecified = "unspecified";
	var Argument = "argument";
	var Local = "local";
	var Static = "static";
	var Field = "field";
}

package ijhaxe.debug;

/**
	Error codes the adapter sends as the DAP `Message.id`. Clients branch on the
	code, so it must stay stable; the message text may change freely.

	The values are plain Ints (`to Int`), so they serialize directly into
	`Message.id`. Generic shares its value with RequestDispatcher's
	ERROR_INVALID_REQUEST; codes for evaluate and name resolution start at 2000.
**/
enum abstract DebugErrorCode(Int) to Int {
	/**
		A plain rejection with no specific meaning for the client.
	**/
	var Generic = 1001;

	/**
		A name in an evaluate expression is not a local, a field of `this` or a
		class known to the module. `variables.name` holds the name, so the client
		can resolve it through its own source knowledge (imports) and retry with
		a qualified expression.
	**/
	var UnresolvedName = 2001;
}

package debug;

/**
	Machine-readable identifiers for error responses — sent as the DAP
	`Message.id`. The CODE is the stable contract the client keys off; the
	human-readable `format` text may change freely without breaking clients.

	Underlying values are plain Ints (`to Int`), so they serialize directly into
	`Message.id`. Kept distinct from the transport-level codes in
	RequestDispatcher (1000–1002); evaluate/resolution codes live in the 2000s.
**/
enum abstract DebugErrorCode(Int) to Int {
	/**
		No specific machine meaning — a plain rejection (== ERROR_INVALID_REQUEST).
	**/
	var Generic = 1001;

	/**
		A name in an evaluate expression could not be resolved against the frame
		(not a local, `this` field, or class known to the module). `variables.name`
		carries the offending identifier so the client can try to resolve it with
		its own source knowledge (imports) and re-issue a qualified expression.
	**/
	var UnresolvedName = 2001;
}

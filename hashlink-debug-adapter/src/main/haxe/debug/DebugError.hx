package debug;

import haxe.Exception;

/**
 * Raised for debug-session problems (bad handshake, unsupported protocol,
 * missing debug info, an unresolvable evaluate expression, ...). Carries a
 * human-readable message plus an optional machine-readable `code` and
 * `variables` map that the adapter forwards to the client as a DAP error
 * response (`Message.id` / `Message.variables`). The code — not the message —
 * is the stable contract clients branch on.
 */
class DebugError extends Exception {
	public final code:DebugErrorCode;
	// Machine-readable details for the client, keyed by name (e.g. the offending
	// identifier for UnresolvedName). Null when there is nothing structured to add.
	public final variables:Null<Map<String, String>>;

	public function new(message:String, ?code:DebugErrorCode, ?variables:Map<String, String>) {
		super(message);
		this.code = code == null ? Generic : code;
		this.variables = variables;
	}
}

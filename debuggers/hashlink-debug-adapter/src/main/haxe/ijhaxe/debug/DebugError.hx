package ijhaxe.debug;

import haxe.Exception;

/**
	An error in a debug session, such as a bad handshake, missing debug info or
	an evaluate expression that cannot be resolved. The adapter sends it to the
	client as a DAP error response: the message as text, `code` as
	`Message.id` and `variables` as `Message.variables`. Clients branch on the
	code, never on the message text.
**/
class DebugError extends Exception {
	public final code:DebugErrorCode;

	// Structured details for the client, keyed by name (for UnresolvedName, the
	// identifier that failed). Null when there is nothing structured to add.
	public final variables:Null<Map<String, String>>;

	public function new(message:String, ?code:DebugErrorCode, ?variables:Map<String, String>) {
		super(message);
		this.code = code == null ? Generic : code;
		this.variables = variables;
	}
}

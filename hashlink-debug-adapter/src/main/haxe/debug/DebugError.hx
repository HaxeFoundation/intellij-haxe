package debug;

import haxe.Exception;

/**
 * Raised for unrecoverable debug-session problems (bad handshake, unsupported
 * protocol, missing debug info). Carries a human-readable message that the
 * adapter forwards to the client as an error response.
 */
class DebugError extends Exception {}

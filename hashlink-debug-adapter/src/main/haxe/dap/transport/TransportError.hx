package dap.transport;

import haxe.Exception;

/**
 * Raised when a DAP frame cannot be decoded (malformed header).
 */
class TransportError extends Exception {}

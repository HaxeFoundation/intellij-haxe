package dap.transport;

import haxe.io.Encoding;
import haxe.io.Input;

/**
 * Reads DAP frames from a haxe.io.Input and returns their JSON payloads.
 * Works on any Input (socket or in-memory), which keeps it unit-testable.
 */
class MessageReader {
	final input:Input;

	public function new(input:Input) {
		this.input = input;
	}

	/**
	 * Blocks until one full frame is available and returns its JSON payload.
	 * Throws haxe.io.Eof when the stream ends, or TransportError on a malformed header.
	 */
	public function read():String {
		var header = readHeaderBlock();
		var length = FrameCodec.parseContentLength(header);
		if (length < 0) {
			throw new TransportError("Missing or invalid Content-Length header: " + header);
		}
		var body = input.read(length);
		return body.getString(0, length, Encoding.UTF8);
	}

	/** Consumes bytes up to and including the CRLF CRLF terminator, returning the header text. */
	function readHeaderBlock():String {
		var buf = new StringBuf();
		// tracks how much of the CR LF CR LF terminator has been matched
		var matched = 0;
		while (true) {
			var b = input.readByte();
			var expected = (matched == 0 || matched == 2) ? FrameCodec.CR : FrameCodec.LF;
			if (b == expected) {
				matched++;
				if (matched == 4) {
					return buf.toString();
				}
			} else {
				matched = (b == FrameCodec.CR) ? 1 : 0;
			}
			buf.addChar(b);
		}
	}
}

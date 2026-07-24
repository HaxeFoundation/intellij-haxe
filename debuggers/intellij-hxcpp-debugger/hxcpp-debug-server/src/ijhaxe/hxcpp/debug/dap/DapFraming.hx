package ijhaxe.hxcpp.debug.dap;

import haxe.io.Bytes;
import haxe.io.BytesBuffer;

/**
	DAP wire framing: `Content-Length: N\r\n\r\n` followed by N bytes of JSON.
	The decoder is incremental — feed() arbitrary chunks as they arrive off the
	socket and take complete payloads out — because TCP has no message
	boundaries. A malformed header is unrecoverable (there is no way to resync
	on a byte stream), so it throws and the caller drops the connection.
**/
class DapFraming {
	// sanity cap: no legitimate DAP request is this big; a huge length is a
	// corrupt header and must not make us allocate gigabytes
	static inline var MAX_PAYLOAD = 16 * 1024 * 1024;
	static inline var HEADER_END = "\r\n\r\n";
	static inline var LENGTH_PREFIX = "Content-Length:";

	var buffered:Bytes = Bytes.alloc(0);

	public function new() {}

	public static function encode(payload:String):Bytes {
		var body = Bytes.ofString(payload);
		var head = Bytes.ofString("Content-Length: " + body.length + HEADER_END);
		var out = new BytesBuffer();
		out.addBytes(head, 0, head.length);
		out.addBytes(body, 0, body.length);
		return out.getBytes();
	}

	/**
		Buffers `chunk` and returns every complete message payload now available
		(zero or more). Throws String on an unparsable header.
	**/
	public function feed(chunk:Bytes):Array<String> {
		var combined = new BytesBuffer();
		combined.addBytes(buffered, 0, buffered.length);
		combined.addBytes(chunk, 0, chunk.length);
		buffered = combined.getBytes();

		var messages:Array<String> = [];
		while (true) {
			var headerEnd = indexOf(buffered, HEADER_END);
			if (headerEnd < 0) {
				break;
			}
			var header = buffered.getString(0, headerEnd);
			var length = contentLength(header);
			if (length < 0 || length > MAX_PAYLOAD) {
				throw "Invalid DAP header: " + header;
			}
			var bodyStart = headerEnd + HEADER_END.length;
			if (buffered.length < bodyStart + length) {
				break; // body not fully arrived yet
			}
			messages.push(buffered.getString(bodyStart, length));
			buffered = buffered.sub(bodyStart + length, buffered.length - bodyStart - length);
		}
		return messages;
	}

	// The Content-Length value from the header block (other header lines are
	// permitted by the spec and ignored); -1 when absent/garbage.
	static function contentLength(header:String):Int {
		for (line in header.split("\r\n")) {
			if (StringTools.startsWith(line, LENGTH_PREFIX)) {
				var parsed = Std.parseInt(StringTools.trim(line.substr(LENGTH_PREFIX.length)));
				return parsed != null ? parsed : -1;
			}
		}
		return -1;
	}

	static function indexOf(haystack:Bytes, needle:String):Int {
		var limit = haystack.length - needle.length;
		for (i in 0...limit + 1) {
			var found = true;
			for (j in 0...needle.length) {
				if (haystack.get(i + j) != StringTools.fastCodeAt(needle, j)) {
					found = false;
					break;
				}
			}
			if (found) {
				return i;
			}
		}
		return -1;
	}
}

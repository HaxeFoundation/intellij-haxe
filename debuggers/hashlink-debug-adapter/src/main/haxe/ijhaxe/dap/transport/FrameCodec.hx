package ijhaxe.dap.transport;

import haxe.io.Bytes;
import haxe.io.BytesBuffer;
import haxe.io.Encoding;

/**
	Encodes/decodes the DAP wire framing: an ASCII header block
	("Content-Length: <n>") terminated by CRLF CRLF, followed by
	<n> bytes of UTF-8 encoded JSON.

	CR and LF are written as byte values, never as string escapes,
	so the framing is identical on every platform.
**/
class FrameCodec {
	public static inline var CR:Int = 13;
	public static inline var LF:Int = 10;

	/**
		Wraps a JSON payload in a DAP frame. Content-Length counts UTF-8 bytes, not characters.
	**/
	public static function encode(json:String):Bytes {
		var body = Bytes.ofString(json, Encoding.UTF8);
		var buf = new BytesBuffer();
		buf.addString("Content-Length: " + body.length);
		buf.addByte(CR);
		buf.addByte(LF);
		buf.addByte(CR);
		buf.addByte(LF);
		buf.addBytes(body, 0, body.length);
		return buf.getBytes();
	}

	/**
		Extracts the Content-Length value from a header block.
		Tolerates additional headers and is case-insensitive on the header name.
		Returns -1 when no valid Content-Length header is present.
	**/
	public static function parseContentLength(header:String):Int {
		for (line in header.split(String.fromCharCode(LF))) {
			var separator = line.indexOf(":");
			if (separator <= 0) {
				continue;
			}
			var name = StringTools.trim(line.substr(0, separator)).toLowerCase();
			if (name != "content-length") {
				continue;
			}
			var length = Std.parseInt(StringTools.trim(line.substr(separator + 1)));
			if (length != null && length >= 0) {
				return length;
			}
		}
		return -1;
	}
}

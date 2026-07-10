package tests;

import dap.transport.FrameCodec;
import dap.transport.MessageReader;
import haxe.io.Bytes;
import haxe.io.BytesBuffer;
import haxe.io.BytesInput;

class FrameCodecTest {
	static var CRLF = String.fromCharCode(FrameCodec.CR) + String.fromCharCode(FrameCodec.LF);

	public static function run(assert:Assert):Void {
		encodeProducesHeaderAndBody(assert);
		roundTripAscii(assert);
		roundTripMultiByteUtf8(assert);
		multipleFramesInOneStream(assert);
		parseContentLengthVariants(assert);
	}

	static function encodeProducesHeaderAndBody(assert:Assert):Void {
		var frame = FrameCodec.encode("{}");
		var text = frame.getString(0, frame.length);
		assert.equals("Content-Length: 2" + CRLF + CRLF + "{}", text, "encoded frame layout");
	}

	static function roundTripAscii(assert:Assert):Void {
		var payload = "{\"seq\":1,\"type\":\"request\",\"command\":\"initialize\"}";
		var reader = new MessageReader(new BytesInput(FrameCodec.encode(payload)));
		assert.equals(payload, reader.read(), "ascii round trip");
	}

	static function roundTripMultiByteUtf8(assert:Assert):Void {
		// content length must count UTF-8 bytes, not characters
		var payload = "{\"name\":\"æøå\"}";
		var frame = FrameCodec.encode(payload);
		var headerText = frame.getString(0, frame.length);
		var expectedByteLength = Bytes.ofString(payload).length;
		assert.isTrue(headerText.indexOf("Content-Length: " + expectedByteLength) == 0, "content length counts bytes");

		var reader = new MessageReader(new BytesInput(frame));
		assert.equals(payload, reader.read(), "multi-byte round trip");
	}

	static function multipleFramesInOneStream(assert:Assert):Void {
		var first = "{\"seq\":1}";
		var second = "{\"seq\":2}";
		var buf = new BytesBuffer();
		var firstFrame = FrameCodec.encode(first);
		var secondFrame = FrameCodec.encode(second);
		buf.addBytes(firstFrame, 0, firstFrame.length);
		buf.addBytes(secondFrame, 0, secondFrame.length);

		var reader = new MessageReader(new BytesInput(buf.getBytes()));
		assert.equals(first, reader.read(), "first frame of stream");
		assert.equals(second, reader.read(), "second frame of stream");
	}

	static function parseContentLengthVariants(assert:Assert):Void {
		assert.equals(42, FrameCodec.parseContentLength("Content-Length: 42"), "simple header");
		assert.equals(7, FrameCodec.parseContentLength("content-length:7"), "case insensitive, no space");
		assert.equals(5, FrameCodec.parseContentLength("Other: x" + CRLF + "Content-Length: 5"), "extra headers tolerated");
		assert.equals(-1, FrameCodec.parseContentLength("Other: x"), "missing header rejected");
		assert.equals(-1, FrameCodec.parseContentLength("Content-Length: nope"), "non-numeric length rejected");
	}
}

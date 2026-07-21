package tests;

import haxe.io.Bytes;
import intellij.hxcpp.debug.dap.DapFraming;

class FramingTest {
	public static function run(assert:Assert):Void {
		encodeProducesTheDapHeader(assert);
		aMessageSplitAcrossChunksAssembles(assert);
		twoMessagesInOneChunkBothDecode(assert);
		extraHeaderLinesAreTolerated(assert);
		aGarbageHeaderThrows(assert);
	}

	static function encodeProducesTheDapHeader(assert:Assert):Void {
		var encoded = DapFraming.encode('{"a":1}');
		assert.equals('Content-Length: 7\r\n\r\n{"a":1}', encoded.toString(), "encoded frame");
	}

	static function aMessageSplitAcrossChunksAssembles(assert:Assert):Void {
		var framing = new DapFraming();
		var whole = DapFraming.encode('{"seq":1}');
		var first = whole.sub(0, 10);
		var second = whole.sub(10, whole.length - 10);
		assert.equals(0, framing.feed(first).length, "no message from a partial chunk");
		var messages = framing.feed(second);
		assert.equals(1, messages.length, "one message once complete");
		assert.equals('{"seq":1}', messages[0], "payload reassembled");
	}

	static function twoMessagesInOneChunkBothDecode(assert:Assert):Void {
		var framing = new DapFraming();
		var chunk = new haxe.io.BytesBuffer();
		var a = DapFraming.encode('{"seq":1}');
		var b = DapFraming.encode('{"seq":2}');
		chunk.addBytes(a, 0, a.length);
		chunk.addBytes(b, 0, b.length);
		var messages = framing.feed(chunk.getBytes());
		assert.equals(2, messages.length, "both messages in one chunk decode");
		assert.equals('{"seq":2}', messages[1], "second payload intact");
	}

	static function extraHeaderLinesAreTolerated(assert:Assert):Void {
		var framing = new DapFraming();
		var raw = Bytes.ofString("Content-Type: application/json\r\nContent-Length: 2\r\n\r\n{}");
		var messages = framing.feed(raw);
		assert.equals(1, messages.length, "extra header line ignored");
		assert.equals("{}", messages[0], "payload after multi-line header");
	}

	static function aGarbageHeaderThrows(assert:Assert):Void {
		var framing = new DapFraming();
		var threw = false;
		try {
			framing.feed(Bytes.ofString("HELLO WORLD\r\n\r\n"));
		} catch (e:Dynamic) {
			threw = true;
		}
		assert.isTrue(threw, "unparsable header is fatal for the connection");
	}
}

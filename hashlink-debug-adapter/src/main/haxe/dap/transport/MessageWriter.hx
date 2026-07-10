package dap.transport;

import haxe.io.Output;

/**
 * Writes JSON payloads as DAP frames to a haxe.io.Output.
 */
class MessageWriter {
	final output:Output;

	public function new(output:Output) {
		this.output = output;
	}

	public function write(json:String):Void {
		output.write(FrameCodec.encode(json));
		output.flush();
	}
}

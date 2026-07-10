package adapter;

import dap.protocol.ProtocolMessage;
import dap.transport.MessageReader;
import dap.transport.MessageWriter;
import haxe.Json;
import sys.net.Socket;
import sys.thread.Deque;
import sys.thread.Thread;

/**
 * Runs one DAP session over a connected socket.
 *
 * Three threads, so the adapter never blocks on any single activity:
 *  - reader thread: reads frames off the socket into the inbound queue
 *  - worker (caller's thread): pops the inbound queue and dispatches
 *  - writer thread: pops the outbound queue and writes frames to the socket
 *
 * A null value on a queue is the shutdown sentinel. Shutdown paths:
 *  - "disconnect" request: worker enqueues the response, then the sentinel;
 *    the writer flushes everything before stopping.
 *  - client vanishes: the reader hits Eof and pushes the sentinel inbound.
 */
class DebugAdapter {
	final socket:Socket;
	final inbound = new Deque<Null<String>>();
	final outbound = new Deque<Null<ProtocolMessage>>();
	final writerDone = new Deque<Bool>();

	public function new(socket:Socket) {
		this.socket = socket;
	}

	/** Serves the session; returns when the client disconnected and all output is flushed. */
	public function run():Void {
		Thread.create(readerLoop);
		Thread.create(writerLoop);

		var dispatcher = new RequestDispatcher(message -> outbound.add(message));
		while (true) {
			var payload = inbound.pop(true);
			if (payload == null) {
				break;
			}
			dispatcher.handleRawPayload(payload);
			if (dispatcher.shutdownRequested) {
				break;
			}
		}

		outbound.add(null);
		writerDone.pop(true);
		try {
			socket.close();
		} catch (e:Dynamic) {}
	}

	function readerLoop():Void {
		var reader = new MessageReader(socket.input);
		try {
			while (true) {
				inbound.add(reader.read());
			}
		} catch (e:Dynamic) {
			// Eof or socket error: tell the worker the client is gone
			inbound.add(null);
		}
	}

	function writerLoop():Void {
		var writer = new MessageWriter(socket.output);
		while (true) {
			var message = outbound.pop(true);
			if (message == null) {
				break;
			}
			try {
				writer.write(Json.stringify(message));
			} catch (e:Dynamic) {
				break;
			}
		}
		writerDone.add(true);
	}
}

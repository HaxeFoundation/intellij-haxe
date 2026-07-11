package adapter;

import debug.DebugSession;
import debug.SessionCommand;
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
 * Threads, so the adapter never blocks on any single activity:
 *  - reader thread: reads client frames into the worker queue
 *  - writer thread: writes outbound frames to the socket
 *  - session thread (created on launch): owns the debuggee and the debug natives
 *  - worker (this thread): the only one that touches the dispatcher
 *
 * Client frames and session events share one inbound queue, so the dispatcher
 * remains single-threaded and total message ordering is preserved.
 */
class DebugAdapter {
	final socket:Socket;
	final inbound = new Deque<WorkerMessage>();
	final outbound = new Deque<Null<ProtocolMessage>>();
	final writerDone = new Deque<Bool>();

	var session:DebugSession;

	public function new(socket:Socket) {
		this.socket = socket;
	}

	/** Serves the session; returns when the client disconnected and all output is flushed. */
	public function run():Void {
		Thread.create(readerLoop);
		Thread.create(writerLoop);

		var dispatcher = new RequestDispatcher(message -> outbound.add(message), command -> dispatchToSession(command));

		while (true) {
			var message = inbound.pop(true);
			switch (message) {
				case ClientPayload(payload):
					dispatcher.handleRawPayload(payload);
				case FromSession(event):
					dispatcher.handleSessionEvent(event);
				case ClientEof:
					// client vanished: if a debuggee is running, tear it down
					if (session != null && !dispatcher.shutdownRequested) {
						dispatchToSession(CmdDisconnect(-1));
					} else {
						break;
					}
			}
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

	function dispatchToSession(command:SessionCommand):Void {
		if (session == null) {
			session = createSession();
			session.start();
		}
		session.send(command);
	}

	function createSession():DebugSession {
		var emit = event -> inbound.add(FromSession(event));
		#if hl
		return new DebugSession(new debug.HlNativeDebugApi(), emit);
		#else
		throw "Debugging is only supported on the HashLink target";
		#end
	}

	function readerLoop():Void {
		var reader = new MessageReader(socket.input);
		try {
			while (true) {
				inbound.add(ClientPayload(reader.read()));
			}
		} catch (e:Dynamic) {
			// Eof or socket error: tell the worker the client is gone
			inbound.add(ClientEof);
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

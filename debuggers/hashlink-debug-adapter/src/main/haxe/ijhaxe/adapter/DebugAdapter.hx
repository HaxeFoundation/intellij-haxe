package ijhaxe.adapter;
import ijhaxe.debug.Trace;

import ijhaxe.debug.session.DebugSession;
import ijhaxe.debug.session.SessionCommand;
import ijhaxe.debug.target.HlNativeDebugApi;

import ijhaxe.dap.protocol.ProtocolMessage;
import ijhaxe.dap.transport.MessageReader;
import ijhaxe.dap.transport.MessageWriter;
import haxe.Json;
import sys.net.Socket;
import sys.thread.Deque;
import sys.thread.Thread;

/**
	Runs one DAP session over a connected socket.

	Threads, so the adapter never blocks on any single activity:
	 - reader thread: reads client frames into the worker queue
	 - writer thread: writes outbound frames to the socket
	 - session thread (created on launch): owns the debuggee and the debug natives
	 - worker (this thread): the only one that touches the dispatcher

	Client frames and session events share one inbound queue, so the dispatcher
	remains single-threaded and total message ordering is preserved.
**/
class DebugAdapter {
	final socket:Socket;
	final inbound = new Deque<WorkerMessage>();
	final outbound = new Deque<Null<ProtocolMessage>>();
	final writerDone = new Deque<Bool>();

	var session:DebugSession;

	public function new(socket:Socket) {
		this.socket = socket;
	}

	static inline var CLIENT_CLOSE_TIMEOUT_MS = 2000;
	static inline var CLIENT_CLOSE_POLL_MS = 10;

	var clientEofSeen = false;

	/**
		Serves the session; returns when the client disconnected and all output is flushed.
	**/
	public function run():Void {
		Thread.create(readerLoop);
		Thread.create(writerLoop);

		var dispatcher = new RequestDispatcher(message -> outbound.add(message), command -> dispatchToSession(command));

		while (true) {
			var message = inbound.pop(true);
			switch (message) {
				case ClientPayload(payload):
					// runs for every message: don't pay preview() + concat when tracing is off
					if (Trace.isEnabled()) {
						Trace.log("recv " + preview(payload));
					}
					dispatcher.handleRawPayload(payload);
				case FromSession(event):
					dispatcher.handleSessionEvent(event);
				case ClientEof:
					clientEofSeen = true;
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
		// Let the CLIENT close the connection first. Closing (or exiting) while
		// the peer has not yet consumed the final response can degenerate into a
		// TCP RST on Windows, and an RST DISCARDS data already buffered on the
		// receiving side - observed as the intermittently lost disconnect
		// response ("Connection reset" on the client while the adapter had
		// already logged the response as sent). EOF from our reader means the
		// client received everything and closed; the timeout covers clients
		// that never close.
		awaitClientClose();
		try {
			socket.close();
		} catch (e:Dynamic) {}
	}

	function awaitClientClose():Void {
		var waited = 0;
		while (!clientEofSeen && waited < CLIENT_CLOSE_TIMEOUT_MS) {
			var message = inbound.pop(false);
			switch (message) {
				case null:
					Sys.sleep(CLIENT_CLOSE_POLL_MS / 1000);
					waited += CLIENT_CLOSE_POLL_MS;
				case ClientEof:
					clientEofSeen = true;
				default:
					// late messages after shutdown: nothing left to serve them
			}
		}
		Trace.log(clientEofSeen ? "client closed; exiting" : "client did not close within timeout; exiting");
	}

	// First ~100 chars: enough to identify command/seq without flooding the pipe.
	static function preview(json:String):String {
		return json.length <= 100 ? json : json.substr(0, 100) + "…";
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
		return new DebugSession(new HlNativeDebugApi(), emit);
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
				var json = Json.stringify(message);
				writer.write(json);
				// runs for every message: don't pay preview() + concat when tracing is off
				if (Trace.isEnabled()) {
					Trace.log("sent " + preview(json));
				}
			} catch (e:Dynamic) {
				Trace.log("writer failed: " + Std.string(e));
				break;
			}
		}
		writerDone.add(true);
	}
}

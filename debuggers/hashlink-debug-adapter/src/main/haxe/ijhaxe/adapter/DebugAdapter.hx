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

	Four threads keep the adapter from blocking on any single activity:
	 - the reader thread reads client frames into the inbound queue;
	 - the writer thread writes outbound frames to the socket;
	 - the session thread (created at launch) owns the debuggee and the debug natives;
	 - the worker (the thread calling `run`) is the only one that touches the dispatcher.

	Client frames and session events share one inbound queue, so the dispatcher
	stays single-threaded and sees all messages in one order.
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
					// this runs for every message: build the preview only when tracing
					if (Trace.isEnabled()) {
						Trace.log("recv " + preview(payload));
					}
					dispatcher.handleRawPayload(payload);
				case FromSession(event):
					dispatcher.handleSessionEvent(event);
				case ClientEof:
					clientEofSeen = true;
					// the client is gone: tear down a debuggee that is still running
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
		// Let the client close the connection first. On Windows, closing (or
		// exiting) before the client has read the final response can turn into
		// a TCP RST, and an RST discards data already buffered at the receiver:
		// the client sees "Connection reset" and loses the disconnect response.
		// EOF on the reader means the client has read everything and closed.
		// The timeout covers clients that never close.
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
					// messages arriving after shutdown are dropped
			}
		}
		Trace.log(clientEofSeen ? "client closed; exiting" : "client did not close within timeout; exiting");
	}

	// The first 100 characters identify the command and seq without flooding the trace.
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
			// EOF or a socket error: tell the worker the client is gone
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
				// this runs for every message: build the preview only when tracing
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

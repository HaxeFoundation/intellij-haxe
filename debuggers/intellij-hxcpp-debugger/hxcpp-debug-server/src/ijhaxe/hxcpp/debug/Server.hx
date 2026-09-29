package ijhaxe.hxcpp.debug;

#if (cpp && HXCPP_DEBUGGER)
import haxe.io.Bytes;
import haxe.io.Eof;
import ijhaxe.hxcpp.debug.DebuggerApi;
import ijhaxe.hxcpp.debug.dap.DapFraming;
import sys.net.Host;
import sys.net.Socket;
import sys.thread.Deque;
import sys.thread.Thread;

/**
	The DAP debug server that runs inside the debuggee. Macro.injectServer
	(called from extraParams.hxml) pulls it into `cpp -debug` builds. Its
	static init runs BEFORE user code: it connects OUT to the IDE, which
	listens on the port it put into the environment variables, and holds the
	main thread until the IDE has finished configuring, so that breakpoints
	exist before user code runs.

	The server must never harm the host program. Without a listener, with a
	bad configuration or after any connection failure, the program simply
	runs undebugged. An unconfigured build (no environment variables, no
	defines) makes exactly one quick connection attempt.
**/
@:keep
class Server {
	static inline var CONFIGURED_CONNECT_ATTEMPTS = 40; // about 10s while the IDE starts listening
	static inline var CONNECT_RETRY_DELAY_S = 0.25;
	static inline var POLL_TIMEOUT_S = 0.05;
	static inline var READ_CHUNK_BYTES = 4096;

	// A diagnostic log, written to the file named by HXCPP_DEBUG_LOG and off
	// when the variable is unset. The protocol is opaque and multi-threaded,
	// so a plain append log is the practical way to trace a live session. The
	// serve loop also logs every protocol message, so when a session hangs,
	// the last logged line names the request that caused it.
	static var logEnabled:Null<Bool> = null;

	public static function log(msg:String):Void {
		if (logEnabled == null) {
			logEnabled = Sys.getEnv("HXCPP_DEBUG_LOG") != null;
		}
		if (!logEnabled) {
			return;
		}
		var path = Sys.getEnv("HXCPP_DEBUG_LOG");
		try {
			var out = sys.io.File.append(path, false);
			out.writeString(Std.string(Sys.time()) + " " + msg + "\n");
			out.close();
		} catch (e:Dynamic) {}
	}

	static function __init__():Void {
		try {
			start();
		} catch (e:Dynamic) {
			log("start threw: " + Std.string(e));
		}
	}

	static function start():Void {
		var config = Config.resolve(Sys.getEnv,
			Macro.definedValue("HXCPP_DEBUG_HOST", null),
			Macro.definedValue("HXCPP_DEBUG_PORT", null));
		var socket = connect(config);
		if (socket == null) {
			return; // nobody listening: run undebugged
		}
		// Runs on the MAIN thread. The event handler must be installed ON THE
		// SERVER THREAD, because setEventNotificationHandler registers its
		// caller as hxcpp's debug thread, the one thread that never breaks.
		// Installed from main, main would never hit a breakpoint. So the server
		// thread is spawned first; once it has registered the handler and
		// excluded itself (`ready`), main enables its own debugging. Only then
		// can main break, with the handler already in place to report the stop.
		var api:DebuggerApi = new NativeDebuggerApi();
		var events = new Deque<DebugEvent>();
		var ready = new Deque<Bool>();
		var released = new Deque<Bool>();

		Thread.create(() -> run(api, events, socket, released, ready));
		ready.pop(true);
		api.enableCurrentThread();
		// hold user code until the IDE has finished configuring or the connection is lost
		released.pop(true);
	}

	static function connect(config:Config.ServerConfig):Null<Socket> {
		var attempts = config.configured ? CONFIGURED_CONNECT_ATTEMPTS : 1;
		for (attempt in 0...attempts) {
			var socket = new Socket();
			try {
				socket.connect(new Host(config.host), config.port);
				return socket;
			} catch (e:Dynamic) {
				try {
					socket.close();
				} catch (e2:Dynamic) {}
				if (attempt < attempts - 1) {
					Sys.sleep(CONNECT_RETRY_DELAY_S);
				}
			}
		}
		return null;
	}

	// The server thread: the ONLY thread that does protocol I/O and the only
	// caller into the Dispatcher. The event handler runs on the threads that
	// raise runtime events and only queues them; this thread takes them from
	// the queue and handles them.
	static function run(api:DebuggerApi, events:Deque<DebugEvent>, socket:Socket, released:Deque<Bool>, ready:Deque<Bool>):Void {
		// Registering the handler here makes THIS server thread hxcpp's debug
		// thread, the one thread that never breaks. Main is then signalled that
		// the handler is in place, so it may enable its own debugging.
		api.setEventHandler(event -> events.add(event));
		api.excludeCurrentThread();
		ready.add(true);

		var framing = new DapFraming();
		var dispatcher = new Dispatcher(api, payload -> {
			log("OUT " + payload);
			var frame = DapFraming.encode(payload);
			socket.output.writeFullBytes(frame, 0, frame.length);
		});

		var releasedMain = false;
		function releaseMain():Void {
			if (!releasedMain) {
				releasedMain = true;
				released.add(true);
			}
		}

		try {
			var buffer = Bytes.alloc(READ_CHUNK_BYTES);
			while (!dispatcher.shutdownRequested) {
				while (true) {
					var event = events.pop(false);
					if (event == null) {
						break;
					}
					try {
						dispatcher.handleDebugEvent(event);
					} catch (e:Dynamic) {
						// the same fault isolation as for requests: a faulting stop
						// handler (a condition evaluated over a corrupt frame, for
						// example) must not end the serve loop
						log("event handling failed: " + Std.string(e));
					}
				}
				// Readability is polled with select rather than a read timeout: on
				// cpp a timed-out blocking read can surface as Eof, which looks
				// exactly like a real close. select keeps the loop running, so it
				// drains events while no request is pending.
				var selected = Socket.select([socket], null, null, POLL_TIMEOUT_S);
				if (selected.read.length > 0) {
					var read = socket.input.readBytes(buffer, 0, READ_CHUNK_BYTES); // Eof here means a real close
					if (read > 0) {
						for (payload in framing.feed(buffer.sub(0, read))) {
							log("IN  " + payload);
							dispatcher.handleRequest(payload);
							log("DONE");
						}
					}
				}
				if (dispatcher.configurationDone) {
					releaseMain();
				}
			}
		} catch (e:Dynamic) {
			// Eof (the IDE went away) or a connection error: stop serving and let
			// the program run. The reason is logged, because from the IDE side a
			// silent exit here looks like a frozen session.
			log("serve loop ended: " + Std.string(e));
		}
		releaseMain(); // never leave the main thread frozen
		try {
			socket.close();
		} catch (e:Dynamic) {}
	}

}
#end

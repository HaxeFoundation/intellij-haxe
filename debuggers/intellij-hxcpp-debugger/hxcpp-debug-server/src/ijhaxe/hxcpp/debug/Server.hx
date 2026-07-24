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
	The in-debuggee DAP debug server. Pulled into `cpp -debug` builds by
	Macro.injectServer (see extraParams.hxml); its static init runs BEFORE user
	code, connects OUT to the IDE (which listens on the ephemeral port it put
	into the env vars) and holds the main thread until the IDE has finished
	configuring (breakpoints must exist before user code runs).

	Robustness rule: never harm the host program. No listener, bad config, or
	any wire failure means the app simply runs undebugged; an unconfigured
	build (no env vars, no defines) makes exactly one quick connect attempt.
**/
@:keep
class Server {
	static inline var CONFIGURED_CONNECT_RETRIES = 40; // ~10s while the IDE spins up
	static inline var CONNECT_RETRY_DELAY_S = 0.25;
	static inline var POLL_TIMEOUT_S = 0.05;
	static inline var READ_CHUNK = 4096;

	// A gated diagnostic log: writes to the file named by HXCPP_DEBUG_LOG, off
	// otherwise. The debug protocol is opaque and multi-threaded, so a low-tech
	// append log is the practical way to trace a live session. With the env var
	// set, the server also logs every wire message (see run's loop) — when a
	// session wedges, the last logged line names the request that killed it.
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
		// SERVER THREAD, because setEventNotificationHandler registers its caller
		// as hxcpp's debug thread (the one thread excluded from breaking). Set it
		// on main and main would never hit a breakpoint. So: spawn the server
		// thread, wait for it to register the handler and exclude itself (`ready`),
		// THEN enable this (main) thread's debugging — only now can it break, with
		// the handler already in place to report the stop.
		var api:DebuggerApi = new NativeDebuggerApi();
		var events = new Deque<DebugEvent>();
		var ready = new Deque<Bool>();
		var released = new Deque<Bool>();
		Thread.create(() -> run(api, events, socket, released, ready));
		ready.pop(true);
		api.enableCurrentThread();
		// hold user code until the IDE finished configuring (or the wire died)
		released.pop(true);
	}

	static function connect(config:Config.ServerConfig):Null<Socket> {
		var attempts = config.configured ? CONFIGURED_CONNECT_RETRIES : 1;
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

	// The server thread: the ONLY thread doing protocol I/O and the only
	// caller into the Dispatcher. Runtime events arrive queued from the
	// stopping threads (via the handler installed on the main thread) and are
	// enriched (stop status) here.
	static function run(api:DebuggerApi, events:Deque<DebugEvent>, socket:Socket, released:Deque<Bool>, ready:Deque<Bool>):Void {
		// Registering the handler here makes THIS (the server) thread hxcpp's
		// debug thread — the one thread excluded from breaking. Then signal main
		// that the handler is in place so it may enable its own debugging.
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
			var buffer = Bytes.alloc(READ_CHUNK);
			while (!dispatcher.shutdownRequested) {
				while (true) {
					var event = events.pop(false);
					if (event == null) {
						break;
					}
					try {
						dispatcher.handleDebugEvent(event);
					} catch (e:Dynamic) {
						// same fault isolation as requests: a faulting stop handler
						// (e.g. a condition evaluated over a corrupt frame) must not
						// kill the serve loop
						log("event handling failed: " + Std.string(e));
					}
				}
				// Poll readability with select rather than a read timeout: a
				// timed-out blocking read can surface as Eof on cpp, which is
				// indistinguishable from a real close. select keeps the loop
				// spinning to drain events while no request is pending.
				var selected = Socket.select([socket], null, null, POLL_TIMEOUT_S);
				if (selected.read.length > 0) {
					var read = socket.input.readBytes(buffer, 0, READ_CHUNK); // Eof here = real close
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
			// Eof (IDE went away) or a wire error: stop serving, let the app run.
			// Logged because a silent exit here looks like a frozen session from
			// the IDE side — the log names what actually ended the loop.
			log("serve loop ended: " + Std.string(e));
		}
		releaseMain(); // never leave the main thread frozen
		try {
			socket.close();
		} catch (e:Dynamic) {}
	}

}
#end

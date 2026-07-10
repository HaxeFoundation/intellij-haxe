package adapter;

import sys.net.Host;
import sys.net.Socket;

/**
 * Entry point of the HashLink DAP debug adapter.
 *
 * Usage: hl hl-debug-adapter.hl [--port <n>]
 *
 * Binds a TCP server on 127.0.0.1 (port 0 = OS-assigned, the default),
 * announces the chosen port on stdout as "DAP-ADAPTER-LISTENING:<port>",
 * then serves exactly one DAP client session and exits.
 */
class Main {
	static inline var LISTENING_PREFIX = "DAP-ADAPTER-LISTENING:";

	static function main():Void {
		var port = parsePort(Sys.args());

		var server = new Socket();
		server.bind(new Host("127.0.0.1"), port);
		server.listen(1);

		Sys.println(LISTENING_PREFIX + server.host().port);
		Sys.stdout().flush();

		var client = server.accept();
		new DebugAdapter(client).run();

		server.close();
		Sys.exit(0);
	}

	static function parsePort(args:Array<String>):Int {
		for (i in 0...args.length) {
			if (args[i] == "--port" && i + 1 < args.length) {
				var port = Std.parseInt(args[i + 1]);
				if (port == null || port < 0 || port > 65535) {
					Sys.stderr().writeString("Invalid --port value: " + args[i + 1]);
					Sys.exit(2);
				}
				return port;
			}
		}
		return 0;
	}
}

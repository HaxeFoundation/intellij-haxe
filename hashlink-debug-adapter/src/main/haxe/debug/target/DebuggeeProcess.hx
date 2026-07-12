package debug.target;

import debug.DebugError;
import debug.session.DebugSession;

import haxe.io.Bytes;
import sys.io.Process;
import sys.net.Host;
import sys.net.Socket;
import sys.thread.Thread;

/**
 * Spawns and owns the HashLink debuggee process, launched under the VM's
 * debug server (`hl --debug <port> --debug-wait <program>`), and pumps its
 * stdout/stderr so the pipes never fill and block the debuggee.
 *
 * Output is delivered through the `onOutput(category, text)` callback from two
 * dedicated pump threads; nothing here touches the debug natives.
 *
 * WINDOWS GOTCHA: this spawn goes through HL's process.c, which sets
 * STARTF_USESHOWWINDOW + SW_HIDE — Windows then overrides the child's FIRST
 * ShowWindow call with SW_HIDE, so a GUI debuggee's window is created but
 * never shown. GUI clients must spawn the debuggee themselves and use attach
 * mode (launch args `attachPid`/`debugPort`); this path remains for headless
 * debuggees and the integration tests.
 */
class DebuggeeProcess {
	public var pid(default, null):Int;

	final process:Process;
	final onOutput:(category:String, text:String) -> Void;

	public function new(hlPath:String, program:String, programArgs:Array<String>, cwd:Null<String>, debugPort:Int,
			onOutput:(category:String, text:String) -> Void) {
		this.onOutput = onOutput;

		var args = ["--debug", Std.string(debugPort), "--debug-wait", program];
		if (programArgs != null) {
			for (a in programArgs) {
				args.push(a);
			}
		}

		// sys.io.Process has no working-directory parameter, so set it around the
		// spawn. The adapter serves one debuggee at a time, so this is safe; we
		// restore immediately afterwards.
		var previousCwd:Null<String> = null;
		if (cwd != null) {
			previousCwd = Sys.getCwd();
			Sys.setCwd(cwd);
		}
		try {
			process = new Process(hlPath, args);
		} catch (e:Dynamic) {
			if (previousCwd != null) {
				Sys.setCwd(previousCwd);
			}
			throw new DebugError('Failed to launch "$hlPath": ' + Std.string(e));
		}
		if (previousCwd != null) {
			Sys.setCwd(previousCwd);
		}

		pid = process.getPid();
	}

	/** Starts the stdout/stderr pump threads. */
	public function startOutputPumps():Void {
		pump(process.stdout, "stdout");
		pump(process.stderr, "stderr");
	}

	function pump(input:haxe.io.Input, category:String):Void {
		Thread.create(() -> {
			var buffer = Bytes.alloc(4096);
			try {
				while (true) {
					// blocks until at least one byte is available; partial reads are fine,
					// so the debuggee can never stall on a full pipe
					var read = blockingRead(input, buffer);
					if (read <= 0) {
						break;
					}
					onOutput(category, buffer.getString(0, read));
				}
			} catch (e:haxe.io.Eof) {
				// stream closed: pump done
			} catch (e:Dynamic) {
				// process gone: pump done
			}
		});
	}

	// The process-pipe read native blocks without yielding to HL's GC, so a
	// parked pump thread would stall collection for every other thread. Mark the
	// thread as being in a blocking section around the read so the GC ignores it.
	// The parked read allocates nothing (it fills a preallocated buffer); only a
	// terminal EOF throws, which ends the pump anyway.
	inline function blockingRead(input:haxe.io.Input, buffer:Bytes):Int {
		#if hl
		hl.Gc.blocking(true);
		var read = 0;
		var error:Dynamic = null;
		try {
			read = input.readBytes(buffer, 0, buffer.length);
		} catch (e:Dynamic) {
			error = e;
		}
		hl.Gc.blocking(false);
		if (error != null) {
			throw error;
		}
		return read;
		#else
		return input.readBytes(buffer, 0, buffer.length);
		#end
	}

	/** Non-blocking exit-code probe; null while the process is still running. */
	public function tryExitCode():Null<Int> {
		return process.exitCode(false);
	}

	/** Blocks until the process exits and returns its code. */
	public function waitExitCode():Int {
		return process.exitCode(true);
	}

	public function kill():Void {
		try {
			process.kill();
		} catch (e:Dynamic) {}
	}

	public function close():Void {
		try {
			process.close();
		} catch (e:Dynamic) {}
	}

	/**
	 * Reserves an ephemeral TCP port on the loopback interface and returns it.
	 * There is an unavoidable race between closing here and the VM binding it;
	 * DebugSession retries the connect to cover it.
	 */
	public static function findFreePort():Int {
		var socket = new Socket();
		socket.bind(new Host("127.0.0.1"), 0);
		var port = socket.host().port;
		socket.close();
		return port;
	}
}

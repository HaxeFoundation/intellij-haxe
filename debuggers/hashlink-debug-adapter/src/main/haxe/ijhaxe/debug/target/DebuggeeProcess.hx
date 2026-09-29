package ijhaxe.debug.target;
import haxe.io.Eof;
import haxe.io.Input;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.session.DebugSession;

import haxe.io.Bytes;
import sys.io.Process;
import sys.net.Host;
import sys.net.Socket;
import sys.thread.Thread;

/**
	Spawns and owns the HashLink debuggee process, launched under the VM's
	debug server (`hl --debug <port> --debug-wait <program>`), and pumps its
	stdout/stderr so the pipes never fill and block the debuggee.

	Two pump threads deliver the output through the `onOutput(category, text)`
	callback; nothing here touches the debug natives.

	Windows trap: this spawn goes through HL's process.c, which sets
	STARTF_USESHOWWINDOW and SW_HIDE. Windows then applies SW_HIDE to the
	child's first ShowWindow call, so a GUI debuggee's window is created but
	never shown. GUI clients must spawn the debuggee themselves and use attach
	mode (the launch arguments `attachPid` and `debugPort`). This path serves
	headless debuggees and the integration tests.
**/
class DebuggeeProcess {
	public var pid(default, null):Int;

	/**
		Set when the VM's first stderr output is its "Could not start debugger
		on port" message: another socket took the debug port after findFreePort
		released it and before the VM bound it. The session then retries the
		launch on a fresh port. Only the first chunk is inspected. The VM writes
		it before the program can run (--debug-wait still holds it), so program
		output with the same words can never set the flag.
	**/
	public var debugBindFailed(default, null) = false;

	var stderrSeen = false;

	final process:Process;
	final onOutput:(category:String, text:String) -> Void;
	// Released by each pump thread when its stream reaches EOF. The pipes of a
	// dead process still hold buffered output, and the session waits for both
	// locks so that output reaches the client before the exited event.
	final stdoutDrained = new sys.thread.Lock();
	final stderrDrained = new sys.thread.Lock();

	public function new(hlPath:String, program:String, programArgs:Array<String>, cwd:Null<String>, debugPort:Int,
			onOutput:(category:String, text:String) -> Void) {
		this.onOutput = onOutput;

		var args = ["--debug", Std.string(debugPort), "--debug-wait", program];
		if (programArgs != null) {
			for (a in programArgs) {
				args.push(a);
			}
		}

		// sys.io.Process has no working-directory parameter, so the adapter's own
		// directory is switched around the spawn and restored right after. This is
		// safe because the adapter serves one debuggee at a time.
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

	/**
		Starts the stdout/stderr pump threads.
	**/
	public function startOutputPumps():Void {
		pump(process.stdout, "stdout", stdoutDrained);
		pump(process.stderr, "stderr", stderrDrained);
	}

	/**
		Blocks until both pumps reach EOF, meaning all buffered output was
		forwarded, or until the per-stream timeout passes. A dead process closes
		its pipes promptly, so the timeout is only a guard. Call this before
		reporting the exit, so no output event follows the exited event.
	**/
	public function awaitOutputDrained(timeoutSec:Float):Void {
		stdoutDrained.wait(timeoutSec);
		stderrDrained.wait(timeoutSec);
	}

	function pump(input:Input, category:String, drained:sys.thread.Lock):Void {
		Thread.create(() -> {
			var buffer = Bytes.alloc(4096);
			try {
				while (true) {
					// blocks until at least one byte is available and accepts a partial
					// read, so the debuggee never stalls on a full pipe
					var read = blockingRead(input, buffer);
					if (read <= 0) {
						break;
					}
					var text = buffer.getString(0, read);
					if (category == "stderr" && !stderrSeen) {
						stderrSeen = true;
						debugBindFailed = text.indexOf("Could not start debugger") >= 0;
					}
					onOutput(category, text);
				}
			} catch (e:Eof) {
				// stream closed: pump done
			} catch (e:Dynamic) {
				// process gone: pump done
			}
			drained.release();
		});
	}

	// The process-pipe read native blocks without reaching a GC safepoint, so a
	// parked pump thread would stall every collection. Marking the read as a
	// blocking section lets the GC run without it. Nothing may allocate inside
	// the section: the read fills a preallocated buffer, and only the final EOF
	// throws, which ends the pump anyway.
	inline function blockingRead(input:Input, buffer:Bytes):Int {
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

	/**
		Non-blocking exit-code probe; null while the process is still running.
	**/
	public function tryExitCode():Null<Int> {
		return process.exitCode(false);
	}

	/**
		Blocks until the process exits and returns its code.
	**/
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
		Finds a free TCP port on the loopback interface. The port is released
		before returning, so another socket may take it before the VM binds it;
		DebugSession then relaunches on a fresh port (see debugBindFailed).
	**/
	public static function findFreePort():Int {
		var socket = new Socket();
		socket.bind(new Host("127.0.0.1"), 0);
		var port = socket.host().port;
		socket.close();
		return port;
	}
}

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

	Output is delivered through the `onOutput(category, text)` callback from two
	dedicated pump threads; nothing here touches the debug natives.

	WINDOWS GOTCHA: this spawn goes through HL's process.c, which sets
	STARTF_USESHOWWINDOW + SW_HIDE — Windows then overrides the child's FIRST
	ShowWindow call with SW_HIDE, so a GUI debuggee's window is created but
	never shown. GUI clients must spawn the debuggee themselves and use attach
	mode (launch args `attachPid`/`debugPort`); this path remains for headless
	debuggees and the integration tests.
**/
class DebuggeeProcess {
	public var pid(default, null):Int;

	/**
		Set when the VM's FIRST stderr output is its "Could not start debugger
		on port" startup banner: the reserved debug port was taken between the
		reservation being released (findFreePort) and the VM binding it. The
		session retries the launch on a fresh port when this is set. Only the
		first chunk is ever inspected - it is emitted before the program can
		run (the debuggee is still held by --debug-wait), so program output
		containing the same words can never set the flag.
	**/
	public var debugBindFailed(default, null) = false;

	var stderrSeen = false;

	final process:Process;
	final onOutput:(category:String, text:String) -> Void;
	// released by each pump thread when its stream reaches EOF; lets the
	// session drain the tail output BEFORE reporting the exit (the pipes of a
	// dead process still hold their buffered bytes, surfacing as the
	// final stdout lines arriving AFTER the exited event under machine load)
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

	/**
		Starts the stdout/stderr pump threads.
	**/
	public function startOutputPumps():Void {
		pump(process.stdout, "stdout", stdoutDrained);
		pump(process.stderr, "stderr", stderrDrained);
	}

	/**
		Blocks until both pumps hit EOF (all buffered output was forwarded) or
		the per-stream timeout passes — a dead process EOFs its pipes promptly,
		so the timeout is a guard, not an expected path. Call BEFORE reporting
		the process's exit so no output event trails the exited event.
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
					// blocks until at least one byte is available; partial reads are fine,
					// so the debuggee can never stall on a full pipe
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

	// The process-pipe read native blocks without yielding to HL's GC, so a
	// parked pump thread would stall collection for every other thread. Mark the
	// thread as being in a blocking section around the read so the GC ignores it.
	// The parked read allocates nothing (it fills a preallocated buffer); only a
	// terminal EOF throws, which ends the pump anyway.
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
		Reserves an ephemeral TCP port on the loopback interface and returns it.
		There is an unavoidable race between closing here and the VM binding it;
		DebugSession retries the connect to cover it.
	**/
	public static function findFreePort():Int {
		var socket = new Socket();
		socket.bind(new Host("127.0.0.1"), 0);
		var port = socket.host().port;
		socket.close();
		return port;
	}
}

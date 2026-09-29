package ijhaxe;

// Under HXCPP_TRACY, Tracy stubs that THROW replace the pollable telemetry
// API; a tracy build is profiled through Tracy's own UI instead.
#if (cpp && HXCPP_TELEMETRY && !HXCPP_TRACY)
import haxe.io.Bytes;
import haxe.io.BytesOutput;

/**
	Streams hxcpp telemetry frames to the IDE as an HXTS session, over the
	socket named by the IJ_HAXE_TELEMETRY environment variable (host:port).
	Without the variable, or on any failure, telemetry stays off and the
	application is never disturbed. Stashing and dumping frames must happen
	on the instrumented main thread, so a haxe.Timer runs them there (lime
	pumps the main event loop). A writer thread owns the socket, so sending
	never stalls a frame. stop() flushes the final frame and briefly waits
	for the writer to drain, because the process usually exits in Sys.exit
	right after.
**/
class TelemetryRun {
	/** A tick can fail once in a while (when it coincides with a major collection); only repeated failures stop the collector. */
	static inline var MAX_TICK_FAILURES = 5;

	static var threadNum = -1;
	static var socket:sys.net.Socket;
	static var queue:sys.thread.Deque<Bytes>;
	static var drained:sys.thread.Lock;
	static var timer:haxe.Timer;
	static var stopped = false;
	static var tickFailures = 0;
	static var emptyDumps = 0;
	/** The end time of the previous window; the next window's sample deltas are scaled to the wall time since then. */
	static var lastStampSeconds:Float = -1;

	public static function tryStart():Void {
		var endpoint = Sys.getEnv("IJ_HAXE_TELEMETRY");
		if (endpoint == null) return;
		var colon = endpoint.lastIndexOf(":");
		if (colon <= 0) return;
		try {
			var port = Std.parseInt(endpoint.substr(colon + 1));
			if (port == null) return;
			socket = new sys.net.Socket();
			socket.connect(new sys.net.Host(endpoint.substr(0, colon)), port);
			socket.setFastSend(true);
		} catch (e:Dynamic) {
			socket = null;
			return;
		}

		try {
			threadNum = CppTelemetry.start();
			queue = new sys.thread.Deque();
			drained = new sys.thread.Lock();
			sys.thread.Thread.create(writerLoop);
			queue.add(header());
			// the runtime stashed a blank frame at start; the first dump discards it
			CppTelemetry.stash();
			timer = new haxe.Timer(16);
			timer.run = tick;
		} catch (e:Dynamic) {
			// a runtime whose telemetry entry points throw (future stubs) must not crash the app
			try socket.close() catch (closeError:Dynamic) {}
			socket = null;
		}
	}

	/** Flushes the final frame and closes the stream; safe to call more than once. */
	public static function stop():Void {
		if (socket == null || stopped) return;
		stopped = true;
		if (timer != null) timer.stop();
		try {
			CppTelemetry.stash();
			shipFrame();
		} catch (e:Dynamic) {}
		queue.add(Bytes.alloc(0)); // end marker: the writer drains, closes and releases `drained`
		drained.wait(0.5);
	}

	static function tick():Void {
		if (stopped) return;
		try {
			CppTelemetry.stash();
			if (shipFrame()) {
				emptyDumps = 0;
			} else {
				// An empty dump is not an error by itself, but a long run of
				// them means the stream has stalled; this is logged once.
				emptyDumps++;
				if (emptyDumps == 60) logError("telemetry dump returned no frame for 60 ticks");
			}
			tickFailures = 0;
		} catch (e:Dynamic) {
			tickFailures++;
			logError("telemetry tick failed (" + tickFailures + "/" + MAX_TICK_FAILURES + "): " + Std.string(e));
			if (tickFailures >= MAX_TICK_FAILURES) {
				logError("telemetry collector stopped - the session keeps what was streamed");
				stopped = true;
				if (timer != null) timer.stop();
				queue.add(Bytes.alloc(0)); // end marker: the writer drains and closes, so the IDE sees a clean end
			}
		}
	}

	/** Writes to stderr, which the run console shows, so a failing collector explains itself there. */
	static function logError(message:String):Void {
		try Sys.stderr().writeString("[ijhaxe] " + message + "\n") catch (e:Dynamic) {}
	}

	/** Queues the stashed frame for sending; false when the runtime had no stashed frame to dump. */
	static function shipFrame():Bool {
		var gcTimes = new Array<Int>();
		var names = new Array<String>();
		var samples = new Array<Int>();
		if (!CppTelemetry.dumpInto(threadNum, gcTimes, names, samples)) return false;

		var stamp = haxe.Timer.stamp();
		normalizeDeltas(samples, stamp);
		var payload = output();
		payload.writeDouble(stamp);
		payload.writeInt32(gcTimes[0]);
		payload.writeInt32(gcTimes[1]);
		payload.writeInt32(CppTelemetry.usedBytes());
		payload.writeInt32(CppTelemetry.reservedBytes());
		payload.writeInt32(names.length);
		for (name in names)
			writeName(payload, name);
		payload.writeInt32(samples.length);
		for (value in samples)
			payload.writeInt32(value);

		var record = output();
		record.writeByte(1); // FRAME
		var bytes = payload.getBytes();
		record.writeInt32(bytes.length);
		record.write(bytes);
		queue.add(record.getBytes());
		lastStampSeconds = stamp;
		return true;
	}

	/**
		Rescales the window's sample deltas from profiler-clock ticks to
		MICROSECONDS that add up to the window's wall time. The runtime's
		roughly 1 ms clock is a Sleep(1) loop whose real period depends on the
		OS timer, so raw tick counts can overshoot or undershoot the wall time
		by a wide margin (a frame can count 130 % of its wall time). The timestamps bounding the window are
		exact. Rounding the running total, rather than each delta, keeps the
		rescaled sum exact.
	**/
	static function normalizeDeltas(samples:Array<Int>, stampSeconds:Float):Void {
		var totalTicks = 0;
		var i = 0;
		while (i < samples.length) {
			i += samples[i] + 1; // each sample is [depth, ids..., delta]
			totalTicks += samples[i];
			i++;
		}
		if (totalTicks <= 0 || lastStampSeconds < 0) return;
		var windowUs = (stampSeconds - lastStampSeconds) * 1000000.0;
		if (windowUs <= 0) return;

		var scale = windowUs / totalTicks;
		var cumulativeTicks = 0;
		var previousUs = 0;
		i = 0;
		while (i < samples.length) {
			i += samples[i] + 1;
			cumulativeTicks += samples[i];
			var cumulativeUs = Math.round(cumulativeTicks * scale);
			samples[i] = cumulativeUs - previousUs;
			previousUs = cumulativeUs;
			i++;
		}
	}

	static function header():Bytes {
		var out = output();
		out.writeString("HXTS");
		out.writeUInt16(1);
		out.writeInt32(1000000); // deltas ship normalized to microseconds
		var now = haxe.Timer.stamp();
		out.writeDouble(now);
		lastStampSeconds = now;
		writeName(out, "hxcpp");
		return out.getBytes();
	}

	static function writeName(out:BytesOutput, name:String):Void {
		var utf8 = Bytes.ofString(name);
		out.writeUInt16(utf8.length);
		out.write(utf8);
	}

	static function output():BytesOutput {
		var out = new BytesOutput();
		out.bigEndian = false;
		return out;
	}

	static function writerLoop():Void {
		while (true) {
			var chunk = queue.pop(true);
			if (chunk == null || chunk.length == 0) break;
			try {
				socket.output.write(chunk);
			} catch (e:Dynamic) {
				logError("telemetry send failed - collector stopped: " + Std.string(e));
				stopped = true;
				break;
			}
		}
		try {
			socket.close();
		} catch (e:Dynamic) {}
		drained.release();
	}
}
#end

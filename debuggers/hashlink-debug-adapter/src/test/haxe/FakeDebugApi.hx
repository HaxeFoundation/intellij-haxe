import debug.Pointer;
import debug.target.DebugApi;
import debug.target.Register;
import debug.target.WaitOutcome;

import haxe.Int64;
import haxe.io.Bytes;

/**
	In-memory DebugApi for unit tests: models the debuggee's memory as a byte
	map and its registers as a per-thread map, with a scripted queue of wait
	outcomes. No OS, no threads — usable under the Haxe interpreter.
**/
class FakeDebugApi implements DebugApi {
	public var started(default, null):Bool = false;
	public var stopped(default, null):Bool = false;
	public var flushed(default, null):Int = 0;
	public var forcedBreaks(default, null):Int = 0;
	public var resumes(default, null):Array<Int> = [];

	final memory:Map<String, Int> = new Map();
	final registers:Map<String, Pointer> = new Map();
	final waitQueue:Array<WaitOutcome> = [];

	public function new() {}

	// --- test setup helpers ---

	public function poke(address:Pointer, value:Int):Void {
		memory.set(memKey(address), value & 0xFF);
	}

	public function peek(address:Pointer):Int {
		var v = memory.get(memKey(address));
		return v == null ? 0 : v;
	}

	public function setRegister(threadId:Int, register:Register, value:Pointer):Void {
		registers.set(regKey(threadId, register), value);
	}

	public function enqueueWait(outcome:WaitOutcome):Void {
		waitQueue.push(outcome);
	}

	// --- DebugApi ---

	public function start(pid:Int):Bool {
		started = true;
		return true;
	}

	public function stop(pid:Int):Void {
		stopped = true;
	}

	public function forceBreak(pid:Int):Bool {
		forcedBreaks++;
		return true;
	}

	public function readMemory(pid:Int, addr:Pointer, buffer:Bytes, size:Int):Bool {
		for (i in 0...size) {
			buffer.set(i, peek(Int64.add(addr, Int64.ofInt(i))));
		}
		return true;
	}

	public function writeMemory(pid:Int, addr:Pointer, buffer:Bytes, size:Int):Bool {
		for (i in 0...size) {
			poke(Int64.add(addr, Int64.ofInt(i)), buffer.get(i));
		}
		return true;
	}

	public function flush(pid:Int, addr:Pointer, size:Int):Bool {
		flushed++;
		return true;
	}

	public function wait(pid:Int, timeoutMs:Int):WaitOutcome {
		if (waitQueue.length == 0) {
			return {result: Timeout, threadId: 0};
		}
		return waitQueue.shift();
	}

	public function resume(pid:Int, threadId:Int):Bool {
		resumes.push(threadId);
		return true;
	}

	public function readRegister(pid:Int, threadId:Int, register:Register):Pointer {
		var v = registers.get(regKey(threadId, register));
		return v == null ? Int64.ofInt(0) : v;
	}

	public function writeRegister(pid:Int, threadId:Int, register:Register, value:Pointer):Bool {
		registers.set(regKey(threadId, register), value);
		return true;
	}

	public function setTargetIs64(is64:Bool):Void {
		// the fake's registers are plain map entries; bitness is irrelevant
	}

	static inline function memKey(address:Pointer):String {
		return Int64.toStr(address);
	}

	static inline function regKey(threadId:Int, register:Register):String {
		return threadId + ":" + (register : Int);
	}
}

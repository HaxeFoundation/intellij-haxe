package debug;

import haxe.Int64;

/**
 * The runtime JIT/memory map the debuggee VM sends over the --debug socket
 * (the "HLD1" handshake). Bridges source opcodes ↔ absolute machine addresses.
 *
 * Wire layout empirically verified against HashLink 1.15 (protocol version 1):
 * see JitInfoReader.
 */
class JitInfo {
	public var is64(default, null):Bool;
	public var boolSize4(default, null):Bool;
	public var threads(default, null):Bool;
	public var winCall(default, null):Bool;
	public var hlVersionMajor(default, null):Int;
	public var hlVersionMinor(default, null):Int;
	public var hlVersionPatch(default, null):Int;
	public var pid(default, null):Int;
	public var threadsPtr(default, null):Pointer;
	public var globalsPtr(default, null):Pointer;
	public var jitCodeBase(default, null):Pointer;
	public var codeSize(default, null):Int;
	public var typesPtr(default, null):Pointer;
	public var structSizes(default, null):Array<Int>;
	public var functions(default, null):Array<JitFunction>;

	// functions sorted by start, for address→function binary search
	var sortedByStart:Array<{start:Int, end:Int, fidx:Int}>;

	public function new(fields:{
		is64:Bool, boolSize4:Bool, threads:Bool, winCall:Bool,
		hlVersionMajor:Int, hlVersionMinor:Int, hlVersionPatch:Int,
		pid:Int, threadsPtr:Pointer, globalsPtr:Pointer, jitCodeBase:Pointer,
		codeSize:Int, typesPtr:Pointer, structSizes:Array<Int>, functions:Array<JitFunction>
	}) {
		is64 = fields.is64;
		boolSize4 = fields.boolSize4;
		threads = fields.threads;
		winCall = fields.winCall;
		hlVersionMajor = fields.hlVersionMajor;
		hlVersionMinor = fields.hlVersionMinor;
		hlVersionPatch = fields.hlVersionPatch;
		pid = fields.pid;
		threadsPtr = fields.threadsPtr;
		globalsPtr = fields.globalsPtr;
		jitCodeBase = fields.jitCodeBase;
		codeSize = fields.codeSize;
		typesPtr = fields.typesPtr;
		structSizes = fields.structSizes;
		functions = fields.functions;
		buildIndex();
	}

	function buildIndex():Void {
		sortedByStart = [];
		for (fidx in 0...functions.length) {
			var fn = functions[fidx];
			sortedByStart.push({start: fn.start, end: fn.start + fn.offsets[fn.nops], fidx: fidx});
		}
		sortedByStart.sort((a, b) -> a.start - b.start);
	}

	/** Absolute machine address of opcode `op` in function `fidx`. */
	public function addressOf(fidx:Int, op:Int):Pointer {
		var fn = functions[fidx];
		return Int64.add(jitCodeBase, Int64.ofInt(fn.start + fn.offsets[op]));
	}

	/** True when `ptr` lies within the JIT code region. */
	public function isCodePtr(ptr:Pointer):Bool {
		var rel = Int64.sub(ptr, jitCodeBase);
		if (Int64.isNeg(rel)) {
			return false;
		}
		return Int64.compare(rel, Int64.ofInt(codeSize)) < 0;
	}

	/**
	 * Maps a machine address back to (function index, opcode index).
	 * Returns null when the address is not inside any known function's code.
	 */
	public function resolveAddress(ptr:Pointer):Null<{fidx:Int, op:Int}> {
		if (!isCodePtr(ptr)) {
			return null;
		}
		var rel = Int64.toInt(Int64.sub(ptr, jitCodeBase)); // fits in Int (< codeSize)
		var fidx = functionAtOffset(rel);
		if (fidx < 0) {
			return null;
		}
		var fn = functions[fidx];
		var relInFn = rel - fn.start;
		// last opcode whose offset is <= relInFn
		var op = 0;
		for (i in 0...fn.nops) {
			if (fn.offsets[i] <= relInFn && relInFn < fn.offsets[i + 1]) {
				op = i;
				break;
			}
		}
		return {fidx: fidx, op: op};
	}

	function functionAtOffset(rel:Int):Int {
		var lo = 0;
		var hi = sortedByStart.length - 1;
		while (lo <= hi) {
			var mid = (lo + hi) >> 1;
			var entry = sortedByStart[mid];
			if (rel < entry.start) {
				hi = mid - 1;
			} else if (rel >= entry.end) {
				lo = mid + 1;
			} else {
				return entry.fidx;
			}
		}
		return -1;
	}
}

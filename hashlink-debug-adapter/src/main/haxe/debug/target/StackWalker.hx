package debug.target;

import debug.Pointer;
import debug.module.JitInfo;
import debug.target.DebugApi;

import haxe.Int64;
import haxe.io.Bytes;

/**
 * Reconstructs the call stack of a stopped thread by following the frame-pointer
 * (RBP) chain: the current RIP is the innermost frame, then each saved
 * [RBP] -> caller RBP and [RBP + ptrSize] -> return address gives the next
 * frame, until a return address falls outside JIT code or the depth cap is hit.
 *
 * Only reads memory/registers through DebugApi and resolves addresses through
 * JitInfo, so it is unit-testable with a fake API and a synthetic JitInfo.
 * Returns bytecode coordinates; source mapping is applied by the caller.
 */
class StackWalker {
	static inline var MAX_FRAMES = 64;

	final api:DebugApi;
	final pid:Int;
	final jit:JitInfo;
	final pointerSize:Int;

	public function new(api:DebugApi, pid:Int, jit:JitInfo) {
		this.api = api;
		this.pid = pid;
		this.jit = jit;
		this.pointerSize = jit.is64 ? 8 : 4;
	}

	public function walk(threadId:Int):Array<StackFrameLocation> {
		var frames:Array<StackFrameLocation> = [];

		var eip = api.readRegister(pid, threadId, Eip);
		var ebp = api.readRegister(pid, threadId, Ebp);
		var top = jit.resolveAddress(eip);
		if (top != null) {
			frames.push({fidx: top.fidx, op: top.op, address: eip, ebp: ebp});
		}

		while (frames.length < MAX_FRAMES) {
			if (Int64.eq(ebp, Int64.ofInt(0))) {
				break;
			}
			var returnAddress = readPointer(Int64.add(ebp, Int64.ofInt(pointerSize)));
			var savedEbp = readPointer(ebp);

			if (!jit.isCodePtr(returnAddress)) {
				break;
			}
			var resolved = jit.resolveAddress(returnAddress);
			if (resolved == null) {
				break;
			}
			// the caller executes with base savedEbp
			frames.push({fidx: resolved.fidx, op: resolved.op, address: returnAddress, ebp: savedEbp});

			// caller frame must be at a higher stack address; otherwise stop to avoid loops
			if (Int64.compare(savedEbp, ebp) <= 0) {
				break;
			}
			ebp = savedEbp;
		}

		return frames;
	}

	function readPointer(addr:Pointer):Pointer {
		var buf = Bytes.alloc(pointerSize);
		api.readMemory(pid, addr, buf, pointerSize);
		var low = buf.get(0) | (buf.get(1) << 8) | (buf.get(2) << 16) | (buf.get(3) << 24);
		if (pointerSize == 4) {
			return Int64.make(0, low);
		}
		var high = buf.get(4) | (buf.get(5) << 8) | (buf.get(6) << 16) | (buf.get(7) << 24);
		return Int64.make(high, low);
	}
}

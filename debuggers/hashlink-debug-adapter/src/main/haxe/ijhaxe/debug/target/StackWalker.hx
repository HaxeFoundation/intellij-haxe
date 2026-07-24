package ijhaxe.debug.target;

import ijhaxe.debug.HostPlatform;
import ijhaxe.debug.Pointer;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.target.DebugApi;

import haxe.Int64;
import haxe.io.Bytes;

/**
	Reconstructs the call stack of a stopped thread by following the frame-pointer
	(RBP) chain: the current RIP is the innermost frame, then each saved
	[RBP] -> caller RBP and [RBP + ptrSize] -> return address gives the next
	frame, until a return address falls outside JIT code or the depth cap is hit.

	Only reads memory/registers through DebugApi and resolves addresses through
	JitInfo, so it is unit-testable with a fake API and a synthetic JitInfo.
	Returns bytecode coordinates; source mapping is applied by the caller.
**/
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

	/**
		Set by DebugSession once the exception control exists: the VM's own
		throw-time stack capture for a thread (exc_stack_trace, top first).
		Enables the linux VM-capture recovery in seedFromCEntry.
	**/
	public var capturedStack:Null<Int->Array<Pointer>> = null;

	public function walk(threadId:Int):Array<StackFrameLocation> {
		var frames:Array<StackFrameLocation> = [];

		var eip = api.readRegister(pid, threadId, Eip);
		var ebp = api.readRegister(pid, threadId, Ebp);
		var top = jit.resolveAddress(eip);
		if (top != null) {
			// normal stop: RIP is inside a jitted function
			frames.push({fidx: top.fidx, op: top.op, address: eip, ebp: ebp});
		} else {
			// stopped inside a C runtime function (e.g. an INT3 on hl_throw's
			// entry): RBP still belongs to the caller, so seed the walk from the
			// return-address chain and continue with the first JIT frame as base.
			ebp = seedFromCEntry(threadId, ebp, frames);
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

	/**
		Seeds the walk when the thread is stopped at a C function's ENTRY (before
		its prologue ran, so RBP is still the caller's). Finds the first return
		address that lands in JIT code — the throwing Haxe frame — pushes it, and
		returns the RBP the outer walk should continue from (that frame's base).

		At a C entry the immediate caller's resume address is at [Esp] and its
		frame base is the current RBP; each further C frame is unwound through its
		RBP chain. Returns 0 (walk stops) when no JIT frame is found.
	**/
	function seedFromCEntry(threadId:Int, ebp:Pointer, frames:Array<StackFrameLocation>):Pointer {
		var esp = api.readRegister(pid, threadId, Esp);
		var returnAddress = readPointer(esp); // the immediate caller's resume address
		var frameBase = ebp; // ... whose frame base is the current RBP
		var guard = 0;
		while (guard++ < MAX_FRAMES) {
			var resolved = jit.resolveAddress(returnAddress);
			if (resolved != null) {
				frames.push({fidx: resolved.fidx, op: resolved.op, address: returnAddress, ebp: frameBase});
				return frameBase; // the outer walk continues from this frame's base
			}
			// a C caller: unwind it through its RBP (== frameBase)
			if (Int64.eq(frameBase, Int64.ofInt(0))) {
				return recoverThroughSignalFrame(threadId, frames);
			}
			returnAddress = readPointer(Int64.add(frameBase, Int64.ofInt(pointerSize)));
			var nextBase = readPointer(frameBase);
			if (Int64.compare(nextBase, frameBase) <= 0) {
				return recoverThroughSignalFrame(threadId, frames); // chain broken
			}
			frameBase = nextBase;
		}
		return recoverThroughSignalFrame(threadId, frames);
	}

	/**
		Linux fallback for stops whose C call chain has no frame pointers: a
		signal-delivered VM error (null access) reaches hl_throw through
		-fomit-frame-pointer C frames the RBP unwind above cannot cross. No
		register or signal frame survives to lean on - hl's SIGSEGV handler
		patches the context and returns (sigreturn dismantles the sigframe
		before the error path runs), and by the throw break the C code has
		repurposed RBP (in practice a heap pointer). What DOES survive is
		the stack itself plus the VM's own throw capture (exc_stack_trace):

		- the top frame's IDENTITY is exc_stack_trace[0];
		- its BASE is found by scanning the stack for the return address into
		  the CALLER's function (exc_stack_trace[1]) - that word sits at
		  [top_rbp+8] by the JIT's frame layout, so the base is one slot
		  below. The candidate must hold a plausible saved-RBP (a stack
		  address above itself) or the scan moves on - spilled copies of code
		  pointers fail that test. The outer walk then chains every caller
		  frame from the recovered base as usual.
	**/
	function recoverThroughSignalFrame(threadId:Int, frames:Array<StackFrameLocation>):Pointer {
		// linux-only by construction: the capture offset in Align is the
		// GLIBC layout, and on Windows the seed's RBP unwind works anyway
		if (capturedStack == null || HostPlatform.IS_WINDOWS) {
			return Int64.ofInt(0);
		}
		var captured = capturedStack(threadId);
		if (captured.length == 0) {
			return Int64.ofInt(0);
		}
		var top = jit.resolveAddress(captured[0]);
		if (top == null) {
			return Int64.ofInt(0);
		}
		var callerFn = captured.length > 1 ? jit.resolveAddress(captured[1]) : null;
		var esp = api.readRegister(pid, threadId, Esp);
		var span = 16384;
		var buf = Bytes.alloc(span);
		if (!api.readMemory(pid, esp, buf, span)) {
			return Int64.ofInt(0);
		}
		var stackLimit = Int64.add(esp, Int64.ofInt(8 * 1024 * 1024));
		var words = span >> 3;
		for (i in 1...words) {
			var word = wordAt(buf, i);
			var resolved = jit.resolveAddress(word);
			if (resolved == null || (callerFn != null && resolved.fidx != callerFn.fidx)) {
				continue;
			}
			var base = Int64.add(esp, Int64.ofInt((i - 1) << 3));
			var savedRbp = wordAt(buf, i - 1); // [base] = the caller's saved RBP
			if (Int64.compare(savedRbp, base) <= 0 || Int64.compare(savedRbp, stackLimit) > 0) {
				continue;
			}
			frames.push({fidx: top.fidx, op: top.op, address: captured[0], ebp: base});
			return base; // the outer walk chains the caller frames from here
		}
		return Int64.ofInt(0);
	}

	function wordAt(buf:Bytes, index:Int):Pointer {
		var base = index << 3;
		var low = buf.get(base) | (buf.get(base + 1) << 8) | (buf.get(base + 2) << 16) | (buf.get(base + 3) << 24);
		var high = buf.get(base + 4) | (buf.get(base + 5) << 8) | (buf.get(base + 6) << 16) | (buf.get(base + 7) << 24);
		return Int64.make(high, low);
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

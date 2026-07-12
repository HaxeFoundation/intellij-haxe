package debug.values;

import debug.target.StackFrameLocation;

/**
 * The per-stop state shared across variable inspection: the lazily-walked
 * per-thread frame caches and the variablesReference registry. All threads are
 * frozen at a stop, so any thread's stack is walked on first request and cached
 * until the next resume.
 *
 * Frame ids AND variablesReferences draw from ONE monotonic counter that is
 * never reset: a stale handle from before a resume resolves to nothing, never
 * aliases a new stop's allocation, and the two id spaces can't collide.
 *
 * Owned by VariableInspector; cleared on every resume (`invalidate`) so a
 * reference never outlives its stop — the GC can move objects between stops.
 */
class StopState {
	static inline var REF_BASE = 1000;

	final frameCaches:Map<Int, Array<CachedFrame>> = new Map(); // threadId -> its frames (with ids)
	final frameHandles:Map<Int, CachedFrame> = new Map(); // frameId -> the frame it names
	final references:Map<Int, RefTarget> = new Map();
	var nextHandle:Int = REF_BASE;

	// The thread the stop landed in — writes and eval-call run only here.
	public var stoppedThreadId(default, null):Int = 0;

	// Set by the owner: walks a thread's stack (StackWalker) on demand.
	public var frameWalker:Null<Int->Array<StackFrameLocation>> = null;

	public function new() {}

	/**
	 * Begins a new stop: drops all per-thread frame caches, frame handles, and
	 * references (their NUMBERS are never reused — see nextHandle). `threadId` is
	 * the thread the stop landed in, the only one writes/eval-call may touch.
	 */
	public function startStop(threadId:Int):Void {
		frameCaches.clear();
		frameHandles.clear();
		references.clear();
		stoppedThreadId = threadId;
	}

	/** Clears every per-stop cache (on resume). */
	public function invalidate():Void {
		frameCaches.clear();
		frameHandles.clear();
		references.clear();
	}

	/** True once a stop has produced at least one frame (any thread walked). */
	public function hasFrames():Bool {
		return frameCaches.iterator().hasNext();
	}

	/**
	 * The frames of `threadId` (walked+cached on first request; all threads are
	 * frozen at a stop). Each carries the globally-unique frame id the client
	 * uses for scopes/variables/evaluate.
	 */
	public function framesFor(threadId:Int):Array<CachedFrame> {
		var cached = frameCaches.get(threadId);
		if (cached != null) {
			return cached;
		}
		var walked = frameWalker == null ? [] : frameWalker(threadId);
		var withIds:Array<CachedFrame> = [];
		for (i in 0...walked.length) {
			var frame:CachedFrame = {frameId: nextHandle++, threadId: threadId, index: i, location: walked[i]};
			frameHandles.set(frame.frameId, frame);
			withIds.push(frame);
		}
		frameCaches.set(threadId, withIds);
		return withIds;
	}

	/** The cached frame with the given id, or null (e.g. a stale handle). */
	public inline function frameAt(frameId:Int):Null<CachedFrame> {
		return frameHandles.get(frameId);
	}

	/** Allocates a fresh variablesReference bound to `target`. */
	public function allocReference(target:RefTarget):Int {
		var reference = nextHandle++;
		references.set(reference, target);
		return reference;
	}

	/** The target a variablesReference resolves to, or null (stale reference). */
	public inline function referenceTarget(reference:Int):Null<RefTarget> {
		return references.get(reference);
	}
}

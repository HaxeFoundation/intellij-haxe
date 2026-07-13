package debug;

import haxe.Int64;

/**
 * A memory address in the debuggee. HashLink addresses are pointer-sized
 * (64-bit on the platforms we target), so we model them as Int64 regardless
 * of the host, and read them as 4 or 8 bytes per the handshake `is64` flag.
 *
 * A zero-cost abstract over Int64: it carries the two operations we do to
 * addresses constantly — `p.offset(n)` (address arithmetic) and `p.isNull()`
 * — as methods, so they no longer need re-implementing in every reader. It
 * converts implicitly to/from Int64, so the raw `Int64.*` helpers still apply.
 */
@:forward(high, low)
abstract Pointer(Int64) from Int64 to Int64 {
	/** The address `delta` bytes past this one. */
	public inline function offset(delta:Int):Pointer {
		return Int64.add(this, Int64.ofInt(delta));
	}

	/** True for the null address (0). */
	public inline function isNull():Bool {
		return Int64.eq(this, Int64.ofInt(0));
	}
}

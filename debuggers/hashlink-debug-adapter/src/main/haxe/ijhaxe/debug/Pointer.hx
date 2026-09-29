package ijhaxe.debug;

import haxe.Int64;

/**
	A memory address in the debuggee. It is an Int64 whatever the debuggee's
	bitness; readers read 4 or 8 bytes depending on the handshake's `is64` flag.

	A zero-cost abstract over Int64 that adds the two common address
	operations, `p.offset(n)` and `p.isNull()`. It converts implicitly to and
	from Int64, so the `Int64.*` helpers still apply.
**/
@:forward(high, low)
abstract Pointer(Int64) from Int64 to Int64 {
	public inline function offset(delta:Int):Pointer {
		return Int64.add(this, Int64.ofInt(delta));
	}

	public inline function isNull():Bool {
		return Int64.eq(this, Int64.ofInt(0));
	}
}

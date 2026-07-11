package debug.layout;

import debug.module.JitInfo;

import format.hl.Data.HLType;

/**
 * Size and alignment rules for HashLink values, mirroring the VM's own layout
 * (a port of vshaxe/hashlink-debugger `hld/Align.hx`, itself matching the JIT).
 * `typeSize` is the in-memory/in-struct width; `stackSize` is the width a value
 * occupies as a stack-passed argument (sub-word and 4-byte types are promoted to
 * a full pointer slot on 64-bit); `padSize` aligns a running offset to a type.
 */
class Align {
	public final is64:Bool;
	public final ptr:Int;
	public final boolSize:Int;

	public function new(is64:Bool, boolSize4:Bool) {
		this.is64 = is64;
		this.ptr = is64 ? 8 : 4;
		this.boolSize = boolSize4 ? 4 : 1;
	}

	public function typeSize(t:HLType):Int {
		return switch (t) {
			case HVoid: 0;
			case HUi8: 1;
			case HUi16: 2;
			case HI32, HF32: 4;
			case HI64, HF64: 8;
			case HBool: boolSize;
			default: ptr;
		}
	}

	public function stackSize(t:HLType):Int {
		return switch (t) {
			case HUi8, HUi16, HBool: ptr;
			case HI32, HF32 if (is64): ptr;
			default: typeSize(t);
		}
	}

	/** Padding to add to a running offset `v` so the next field of type `t` is aligned. */
	public function padSize(v:Int, t:HLType):Int {
		if (t == HVoid) {
			return 0;
		}
		var size = typeSize(t);
		return (-v) & (size - 1); // types are power-of-two sized
	}

	// The C compiler's actual per-kind struct alignments, from the handshake
	// (JitInfo.structSizes, indices 1..7 = HUi8..HBool, 8 = pointer). Null until a
	// handshake is available; padStruct then falls back to padSize.
	public var structSizes:Null<Array<Int>> = null;

	/**
	 * Padding using the C struct alignment rules — what the VM uses for enum
	 * constructor params (hl_pad_struct), which can pack tighter than padSize
	 * (e.g. an i32 param lands at +12, inside the venum header's tail padding).
	 */
	public function padStruct(v:Int, t:HLType):Int {
		if (structSizes == null) {
			return padSize(v, t);
		}
		var index = Type.enumIndex(t); // format HLType order == hl_type_kind
		var size = (index >= 1 && index <= 7) ? structSizes[index] : structSizes[8];
		return size <= 1 ? 0 : (-v) & (size - 1);
	}

	public function isFloat(t:HLType):Bool {
		return switch (t) {
			case HF32, HF64: true;
			default: false;
		}
	}
}

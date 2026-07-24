package debug.layout;

import debug.module.JitInfo;

import format.hl.Data.HLType;

/**
	The CPU-architecture layout descriptor: every value size, alignment and
	C-runtime struct offset that differs between the 32- and 64-bit HashLink VMs
	(and, in future, other CPU families). Resolved ONCE from the handshake's
	`is64` at session start and passed to everything that reads raw debuggee
	memory — the single source of truth, so no reader hardcodes a width or
	branches on bitness inline. (A port of vshaxe/hashlink-debugger `hld/Align.hx`
	for the value-layout parts, extended with the runtime struct offsets.)

	`typeSize` is the in-memory/in-struct width; `stackSize` is the width a value
	occupies as a stack-passed argument (sub-word and 4-byte types are promoted to
	a full pointer slot on 64-bit); `padSize` aligns a running offset to a type.

	The struct offsets below are keyed to the layouts in hl.h. Two kinds:
	 - ptr-RELATIVE fields grow with the pointer (e.g. hl_thread_info tail);
	 - FIXED fields stay put because the C source pads the smaller layout up to
	   the larger's alignment. When adding a raw read, check hl.h for a padding
	   `#ifndef HL_64` / `int __pad` before assuming `ptr`-relative.
**/
class Align {
	public final is64:Bool;
	public final ptr:Int;
	public final boolSize:Int;

	/**
		Offset of a vdynamic's value union (a box's payload). FIXED at +8: hl.h
		pads the 32-bit struct ("int __pad; // force align on 16 bytes for double")
		so the union sits at +8 on BOTH bitnesses — reading it at +ptr on a 32-bit
		debuggee reads the padding.
	**/
	public final dynPayload:Int = 8;

	/**
		Offset of `hl_threads_info.threads` (the hl_thread_info* array). FIXED at
		+8: `int count; bool stopping_world;` then the pointer — 4 + 1 rounded up
		to the pointer's alignment is 8 on both bitnesses.
	**/
	public final threadsArray:Int = 8;

	/**
		Offset of `hl_thread_info.exc_value` — ptr-relative (tail of fixed +8 header + 5 pointers).
	**/
	public final threadExcValue:Int;

	/**
		Offset of `hl_thread_info.flags` — ptr-relative (one pointer past exc_value).
	**/
	public final threadFlags:Int;

	/**
		Offset of `hl_thread_info.exc_stack_count` — the i32 right after flags.
	**/
	public final threadExcStackCount:Int;

	/**
		Offset of `hl_thread_info.exc_stack_trace[0]` on a LINUX glibc debuggee:
		flags/exc_stack_count (two i32s), thread_name[128], then jmp_buf gc_regs —
		sizeof(jmp_buf) is 200 on glibc x86_64 and 156 on glibc x86. Only the
		signal-frame stack recovery reads this, and it self-validates (the entry
		must resolve into JIT code), so a wrong offset degrades to "no frames",
		exactly today's behaviour — never to garbage frames.
	**/
	public final threadExcStackTraceLinux:Int;

	/**
		Offset of `hl_type_obj.name`. SHRINKS on 32-bit: three i32 fields then the
		`const uchar*` name, padded to the pointer's alignment — +16 on 64-bit,
		+12 on 32-bit.
	**/
	public final objTypeName:Int;

	/**
		Stride of one `hl_field_lookup` entry in a vdynobj's lookup table:
		`hl_type* t; int hashed_name; int field_index;` = ptr + 8. (hashed_name @
		+ptr, the packed slot/order i32 @ +ptr+4.)
	**/
	public final fieldLookupStride:Int;

	public function new(is64:Bool, boolSize4:Bool) {
		this.is64 = is64;
		this.ptr = is64 ? 8 : 4;
		this.boolSize = boolSize4 ? 4 : 1;
		this.threadExcValue = ptr * 5 + 8;
		this.threadFlags = ptr * 6 + 8;
		this.threadExcStackCount = threadFlags + 4;
		this.threadExcStackTraceLinux = threadFlags + 8 + 128 + (is64 ? 200 : 156);
		this.objTypeName = is64 ? 16 : 12;
		this.fieldLookupStride = ptr + 8;
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

	/**
		Padding to add to a running offset `v` so the next field of type `t` is aligned.
	**/
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
		Padding using the C struct alignment rules — what the VM uses for enum
		constructor params (hl_pad_struct), which can pack tighter than padSize
		(e.g. an i32 param lands at +12, inside the venum header's tail padding).
	**/
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

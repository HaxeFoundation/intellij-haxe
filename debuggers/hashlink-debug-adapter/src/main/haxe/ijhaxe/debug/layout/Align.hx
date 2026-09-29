package ijhaxe.debug.layout;

import ijhaxe.debug.module.JitInfo;

import format.hl.Data.HLType;

/**
	Every value size, alignment and C runtime struct offset that differs between
	the 32-bit and 64-bit HashLink VMs. It is created once from the handshake at
	session start and passed to everything that reads raw debuggee memory, so
	no reader hardcodes a width or checks the bitness itself. The value sizes
	are a port of vshaxe/hashlink-debugger `hld/Align.hx`; the struct offsets
	are added here.

	`typeSize` is a value's width in memory and inside structs. `stackSize` is
	its width as a stack-passed argument, where 64-bit promotes values of 4
	bytes or less to a full pointer slot. `padSize` is the padding that aligns
	a running offset to a type.

	The struct offsets follow the layouts in hl.h and come in two kinds.
	Pointer-relative offsets grow with the pointer size. Fixed offsets stay the
	same on both bitnesses because the C source pads the 32-bit layout. Before
	adding a raw read, check hl.h for such padding (`#ifndef HL_64`,
	`int __pad`) rather than assuming the offset is pointer-relative.
**/
class Align {
	public final is64:Bool;
	public final ptr:Int;
	public final boolSize:Int;

	/**
		Offset of a vdynamic's value union, which holds a box's payload. It is +8
		on both bitnesses, because hl.h pads the 32-bit struct to align doubles.
		Reading at +ptr on a 32-bit debuggee would read that padding.
	**/
	public final dynPayload:Int = 8;

	/**
		Offset of `hl_threads_info.threads`, the hl_thread_info* array. It is +8
		on both bitnesses: `int count` and `bool stopping_world` come first, and
		the pointer after them aligns to 8.
	**/
	public final threadsArray:Int = 8;

	/**
		Offset of `hl_thread_info.exc_value`: a fixed 8-byte header, then 5 pointers.
	**/
	public final threadExcValue:Int;

	/**
		Offset of `hl_thread_info.flags`, one pointer past exc_value.
	**/
	public final threadFlags:Int;

	/**
		Offset of `hl_thread_info.exc_stack_count`, the i32 right after flags.
	**/
	public final threadExcStackCount:Int;

	/**
		Offset of `hl_thread_info.exc_stack_trace[0]` on a linux glibc debuggee.
		After flags and exc_stack_count come thread_name[128] and the jmp_buf
		gc_regs, which is 200 bytes on x86_64 and 156 on x86. Only the stack
		recovery for signal frames reads it, and that code accepts an entry only
		when it resolves into JIT code. A wrong offset therefore yields no
		frames, never garbage frames.
	**/
	public final threadExcStackTraceLinux:Int;

	/**
		Offset of `hl_type_obj.name`. Three i32 fields precede the `const uchar*`
		name, which aligns to the pointer size: +16 on 64-bit, +12 on 32-bit.
	**/
	public final objTypeName:Int;

	/**
		Size of one `hl_field_lookup` entry in a vdynobj's lookup table:
		`hl_type* t; int hashed_name; int field_index;`, so ptr + 8. The hash
		sits at +ptr and the packed slot/order i32 at +ptr+4.
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

	// The C compiler's struct alignment per type kind, from the handshake
	// (JitInfo.structSizes: indices 1..7 are HUi8..HBool, 8 is a pointer). While
	// it is null, padStruct falls back to padSize.
	public var structSizes:Null<Array<Int>> = null;

	/**
		Padding by the C struct alignment rules, which the VM uses for enum
		constructor params (hl_pad_struct). It can pack tighter than padSize:
		an i32 param lands at +12, inside the tail padding of the venum header.
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

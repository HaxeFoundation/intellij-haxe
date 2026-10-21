package ijhaxe.debug.eval.call;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.MemoryReader;

import format.hl.Data.HLType;
import format.hl.Data.Opcode;
import haxe.Int64;

/**
	Finds what is needed to BOX a primitive into a `vdynamic` in the debuggee:
	the runtime address of `hl_alloc_dynamic` and the primitive's runtime
	`hl_type*`.

	================================ HACK ================================
	The debug handshake exposes neither. `hl_alloc_dynamic` is a JIT-internal
	helper (no findex, not a native), and the primitive type singletons
	(`&hlt_i32`, `&hlt_f64`, `&hlt_bool`) are global C symbols. The JIT does
	emit both as constants for every `OToDyn` opcode (`x = (someInt :
	Dynamic)`). For a non-pointer source, hashlink `jit.c` lowers it to
	`call_native_consts(hl_alloc_dynamic, {src->t}, 1)`, the same
	set-argument-then-call shape as ONew: argument 0 is the primitive's
	hl_type*, followed by `mov (r/e)ax, <hl_alloc_dynamic>` and `call`. The
	argument goes into a register on x86-64 and is pushed on x86. See
	ConstructorResolver for the byte layout.

	The resolver scans `OToDyn` sites whose SOURCE register is a primitive and
	reads the argument and the called function (MachineCode.mineArgThenCall
	picks the architecture's form). The argument is the type pointer of the
	source register's primitive kind, so each site names the kind it boxes. The
	function is the allocator every site shares. The result is one box recipe
	per primitive KIND the program boxes somewhere. This is the same
	dead-code-elimination limit accepted for constructors: a program that never
	boxes a Bool cannot have one boxed by the debugger.

	FRAGILE, like ConstructorResolver. When no pattern is found, boxing reports
	itself as unavailable rather than guessing.
	=====================================================================
**/
class BoxResolver {
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final memory:MemoryReader;

	var allocDynamic:Pointer = Int64.ofInt(0);
	// runtime type pointer per primitive kind (the format lib's HLType constructor index)
	final typeByKind:Map<Int, Pointer> = new Map();
	var scanned = false;

	public function new(module:ModuleDebugInfo, jit:JitInfo, memory:MemoryReader) {
		this.module = module;
		this.jit = jit;
		this.memory = memory;
	}

	/**
		The box recipe for a primitive `kind` (the format lib's HLType constructor
		index, e.g. `Type.enumIndex(HI32)`), or null when the program never boxes
		that primitive (no OToDyn site to mine) or the machine-code pattern is
		absent.
	**/
	public function resolve(kind:Int):Null<{allocDynamic:Pointer, typePointer:Pointer}> {
		if (!scanned) {
			scan();
			scanned = true;
		}
		var typePtr = typeByKind.get(kind);
		if (typePtr == null || Int64.eq(allocDynamic, Int64.ofInt(0))) {
			return null;
		}
		return {allocDynamic: allocDynamic, typePointer: typePtr};
	}

	function scan():Void {
		for (fidx in 0...module.functionCount()) {
			var ops = module.opcodes(fidx);
			var regs = module.registers(fidx);
			for (op in 0...ops.length) {
				var srcKind = toDynPrimitiveKind(ops[op], regs);
				if (srcKind < 0 || typeByKind.exists(srcKind)) {
					continue; // not a primitive OToDyn, or this kind already resolved
				}
				var mined = mineToDyn(fidx, op);
				if (mined != null) {
					allocDynamic = mined.allocFn;
					typeByKind.set(srcKind, mined.typePtr);
				}
			}
		}
	}

	// The primitive kind of an `OToDyn(dst, src)` whose source register is a
	// primitive, or -1. Boxing a primitive calls alloc_dynamic directly, without
	// the null check a pointer source gets first.
	static function toDynPrimitiveKind(opcode:Opcode, regs:Array<HLType>):Int {
		return switch (opcode) {
			case OToDyn(_, src) if (src >= 0 && src < regs.length && isPrimitive(regs[src])):
				Type.enumIndex(regs[src]);
			default:
				-1;
		}
	}

	static function isPrimitive(t:HLType):Bool {
		return switch (t) {
			case HUi8, HUi16, HI32, HI64, HF32, HF64, HBool: true;
			default: false;
		}
	}

	// Reads an OToDyn site's machine code and extracts the primitive's runtime
	// type pointer and the alloc_dynamic address from its set-argument-then-call
	// sequence.
	function mineToDyn(fidx:Int, op:Int):Null<{typePtr:Pointer, allocFn:Pointer}> {
		var start = jit.addressOf(fidx, op);
		var end = jit.addressOf(fidx, op + 1);
		var len = Int64.toInt(Int64.sub(end, start));
		if (len <= 0 || len > MachineCode.MAX_SITE_BYTES) {
			return null;
		}
		var code = memory.read(start, len);
		var i = 0;
		while (i < len) {
			var mined = MachineCode.mineArgThenCall(code, i, len, jit.is64, jit.winCall);
			if (mined != null) {
				return {typePtr: mined.arg, allocFn: mined.fn};
			}
			i++;
		}
		return null;
	}
}

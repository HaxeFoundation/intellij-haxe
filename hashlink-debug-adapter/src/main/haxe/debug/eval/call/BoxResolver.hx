package debug.eval.call;

import debug.Pointer;
import debug.module.JitInfo;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;

import format.hl.Data.HLType;
import format.hl.Data.Opcode;
import haxe.Int64;

/**
 * Recovers what is needed to BOX a primitive into a `vdynamic` in the debuggee:
 * the runtime `hl_alloc_dynamic` address and a primitive's runtime `hl_type*`.
 *
 * ================================ HACK ================================
 * Neither `hl_alloc_dynamic` nor the primitive type singletons (`&hlt_i32`,
 * `&hlt_f64`, `&hlt_bool`) are reachable from the debug handshake — the former
 * is a JIT-internal helper (no findex, not a native), the latter are global C
 * symbols. But the JIT emits both, as constants, for every `OToDyn` opcode
 * (`x = (someInt : Dynamic)`), lowered (hashlink `jit.c`) for a non-pointer
 * source to `call_native_consts(hl_alloc_dynamic, {src->t}, 1)`:
 *
 *     48 B9 <hlt_i32*  : 8>     mov  rcx, <primitive hl_type*>   (win64 arg0)
 *     48 BF <hlt_i32*  : 8>     mov  rdi, <primitive hl_type*>   (SysV arg0)
 *     48 B8 <alloc_dyn : 8>     mov  rax, <hl_alloc_dynamic>
 *     FF D0                     call rax
 *
 * We scan `OToDyn` sites whose SOURCE register is a primitive and read the two
 * imm64s: the primitive's type pointer (matched to the source register's kind,
 * so it is self-verifying) and the shared allocator address. This yields a box
 * recipe per primitive KIND the program actually boxes somewhere — the same
 * DCE-limited scope accepted for constructors: a program that never boxes a
 * Bool cannot have one boxed by the debugger.
 *
 * FRAGILE and x86-64 only (see ConstructorResolver). No pattern → the feature
 * reports itself unavailable rather than guessing.
 * =====================================================================
 */
class BoxResolver {
	// A `mov reg, imm64` is 10 bytes: a 2-byte REX.W+opcode then the 8-byte
	// immediate. The box site's arg-mov (`mov rcx/rdi, type*`) has this shape, so
	// the type pointer sits at +2 and the following `mov rax, alloc ; call rax`
	// begins at +MOV_IMM64_LEN.
	static inline var MOV_IMM64_LEN = 10;
	// Sanity cap on one OToDyn opcode's machine code: real sites are a handful of
	// instructions, so anything larger is not the pattern we mine — skip it.
	static inline var MAX_SITE_BYTES = 256;

	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final memory:MemoryReader;
	var allocDynamic:Pointer = Int64.ofInt(0);
	// per-primitive-kind (format HLType enum index) runtime type pointer
	final typeByKind:Map<Int, Pointer> = new Map();
	var scanned = false;

	public function new(module:ModuleDebugInfo, jit:JitInfo, memory:MemoryReader) {
		this.module = module;
		this.jit = jit;
		this.memory = memory;
	}

	/**
	 * The box recipe for a primitive `kind` (a format HLType enum index, e.g.
	 * `Type.enumIndex(HI32)`), or null when the program never boxes that
	 * primitive (no OToDyn site to mine) or the machine-code pattern is absent.
	 */
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
	// primitive (so boxing it goes straight through alloc_dynamic with no
	// pointer null-check preamble), or -1.
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

	// Reads an OToDyn site's machine code and extracts the source primitive's
	// runtime type pointer and the alloc_dynamic address from the fixed
	// mov(arg0)=type ; mov rax,alloc ; call rax sequence.
	function mineToDyn(fidx:Int, op:Int):Null<{typePtr:Pointer, allocFn:Pointer}> {
		var start = jit.addressOf(fidx, op);
		var end = jit.addressOf(fidx, op + 1);
		var len = Int64.toInt(Int64.sub(end, start));
		if (len <= 0 || len > MAX_SITE_BYTES) {
			return null;
		}
		var code = memory.read(start, len);
		var argMov = jit.winCall ? MachineCode.MOV_RCX : MachineCode.MOV_RDI;
		var i = 0;
		// need room for the arg-mov (MOV_IMM64_LEN) plus the 2-byte start of the mov rax
		while (i + MOV_IMM64_LEN + 2 <= len) {
			if (code.getUInt16(i) == argMov) {
				var allocFn = MachineCode.movRaxImmThenCall(code, i + MOV_IMM64_LEN, len);
				if (allocFn != null) {
					return {typePtr: MachineCode.read64(code, i + 2), allocFn: allocFn};
				}
			}
			i++;
		}
		return null;
	}
}

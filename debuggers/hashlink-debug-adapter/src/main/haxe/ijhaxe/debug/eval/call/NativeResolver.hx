package ijhaxe.debug.eval.call;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.MemoryReader;

import format.hl.Data.Opcode;
import haxe.Int64;

/**
	Finds the runtime address of a HashLink C native (e.g. `alloc_bytes`) by
	DISASSEMBLING a jitted call to it. This is the same hack as
	ConstructorResolver, for the same reason: the debug handshake gives
	addresses only for bytecode functions, and a native's findex cannot be
	turned into an address.

	The JIT compiles a bytecode `OCall` to a native (hashlink `jit.c`,
	`op_call_fun` -> `call_native(m->functions_ptrs[findex])`) to the fixed
	sequence `mov rax, <native>` (48 B8 ..) and `call rax` (FF D0), so the
	native's absolute address is the imm64. The resolver finds any `OCall`
	whose target findex is the native's and reads the address out of that
	opcode's machine code.

	x86 uses `mov eax, imm32; call eax` instead (see MachineCode). When the
	program never calls the native, or the pattern is absent, resolution
	returns null and the caller reports the feature as unavailable.
**/
class NativeResolver {
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final memory:MemoryReader;
	final cache:Map<String, Null<Pointer>> = new Map();

	public function new(module:ModuleDebugInfo, jit:JitInfo, memory:MemoryReader) {
		this.module = module;
		this.jit = jit;
		this.memory = memory;
	}

	/**
		The runtime address of native `name`, or null when it cannot be mined.
	**/
	public function resolve(name:String):Null<Pointer> {
		if (cache.exists(name)) {
			return cache.get(name);
		}
		var addr = find(name);
		cache.set(name, addr);
		return addr;
	}

	function find(name:String):Null<Pointer> {
		var nativeFindex = module.nativeFindexByName(name);
		if (nativeFindex < 0) {
			return null; // this program does not import that native
		}
		for (fidx in 0...module.functionCount()) {
			var ops = module.opcodes(fidx);
			for (op in 0...ops.length) {
				if (callTargetFindex(ops[op]) != nativeFindex) {
					continue;
				}
				var addr = mineCallSite(fidx, op);
				if (addr != null) {
					return addr;
				}
			}
		}
		return null;
	}

	static function callTargetFindex(opcode:Opcode):Int {
		return switch (opcode) {
			case OCall0(_, f), OCall1(_, f, _), OCall2(_, f, _, _), OCall3(_, f, _, _, _),
				OCall4(_, f, _, _, _, _), OCallN(_, f, _): f;
			default: -1;
		}
	}

	// Reads the call site's machine code and returns the native address loaded
	// by its `mov (r/e)ax, imm; call`.
	function mineCallSite(fidx:Int, op:Int):Null<Pointer> {
		var start = jit.addressOf(fidx, op);
		var end = jit.addressOf(fidx, op + 1);
		var len = Int64.toInt(Int64.sub(end, start));
		if (len <= 0 || len > MachineCode.MAX_SITE_BYTES) {
			return null;
		}
		var code = memory.read(start, len);
		var i = 0;
		while (i < len) {
			var addr = MachineCode.mineMovImmThenCall(code, i, len, jit.is64);
			if (addr != null) {
				return addr;
			}
			i++;
		}
		return null;
	}
}

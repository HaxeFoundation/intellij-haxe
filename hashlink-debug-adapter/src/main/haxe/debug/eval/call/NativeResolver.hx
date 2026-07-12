package debug.eval.call;

import debug.Pointer;
import debug.module.JitInfo;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;

import format.hl.Data.Opcode;
import haxe.Int64;

/**
 * Resolves the runtime address of a HashLink C native (e.g. `alloc_bytes`) by
 * DISASSEMBLING a jitted call site — the same hack as ConstructorResolver, and
 * the same reason: the debug handshake exposes addresses for bytecode functions
 * only, and natives have no findex we can turn into an address.
 *
 * A bytecode `OCall` to a native compiles (hashlink `jit.c`,
 * `op_call_fun` -> `call_native(m->functions_ptrs[findex])`) to the fixed
 * sequence `mov rax, <native> (48 B8 ..); call rax (FF D0)` — the native's
 * absolute address is the imm64. So we find any `OCall` whose target findex is
 * the native's, read that opcode's machine code, and pull the address out.
 *
 * x86-64 only; if the native is never called in the program (no site to mine)
 * or the pattern is absent, resolution fails and the caller degrades
 * gracefully. See MachineCode for the shared byte pattern.
 */
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

	/** The runtime address of native `name`, or null when it can't be mined. */
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

	// Reads the call site's machine code and pulls the native address out of the
	// `mov rax, imm64 ; call rax` the JIT emitted for it.
	function mineCallSite(fidx:Int, op:Int):Null<Pointer> {
		var start = jit.addressOf(fidx, op);
		var end = jit.addressOf(fidx, op + 1);
		var len = Int64.toInt(Int64.sub(end, start));
		if (len <= 0 || len > 256) {
			return null;
		}
		var code = memory.read(start, len);
		var i = 0;
		while (i + 12 <= len) {
			var addr = MachineCode.movRaxImmThenCall(code, i, len);
			if (addr != null) {
				return addr;
			}
			i++;
		}
		return null;
	}
}

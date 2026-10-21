package ijhaxe.debug.eval.call;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.MemoryReader;

import format.hl.Data.HLType;
import format.hl.Data.Opcode;
import haxe.Int64;

/**
	Resolves everything needed to run `new SomeClass(args)` in the debuggee.

	================================ HACK ================================
	The debug handshake offers NO supported way to reach HashLink's object
	allocator (`hl_alloc_obj`) or a class's runtime `hl_type*`. It only
	exposes addresses of *bytecode* functions, and `hl_alloc_obj` is a C
	runtime native without a findex. The resolver therefore DISASSEMBLES the
	machine code the JIT emits for an `ONew` opcode.

	Every Haxe `new` compiles to `ONew dst` (allocate) followed by a call to
	the constructor. For an HOBJ/HSTRUCT the JIT lowers `ONew` (hashlink
	`jit.c`: `call_native_consts(hl_alloc_obj, {dst->t}, 1)`) to a fixed
	sequence: load the class type pointer into argument 0, then
	`mov (r/e)ax, <hl_alloc_obj>` and `call`:

	```
	x86-64:  48 B9/BF <typePtr:8>   mov rcx/rdi, <class hl_type*>  (win64/SysV arg0)
	         48 B8 <allocFn:8>      mov rax, <hl_alloc_obj>
	         FF D0                  call rax
	x86:     68 <typePtr:4>         push <class hl_type*>          (cdecl arg0)
	         B8 <allocFn:4>         mov eax, <hl_alloc_obj>
	         FF D0                  call eax
	```

	The resolver scans an `ONew` site for that sequence
	(MachineCode.mineArgThenCall picks the architecture's form) and reads the
	type pointer and the allocator address. The constructor is the target of
	the `OCall*` that follows and takes the new register as its first
	argument. One `ONew SomeClass` site therefore yields everything needed to
	construct the class. Such a site exists only when the program constructs
	the class itself. That dead-code-elimination limit is accepted: a class
	the program never instantiates may have lost its constructor to DCE anyway.

	This is FRAGILE: it depends on the exact instructions the HashLink JIT
	selects. When the pattern is not found, construction reports itself as
	unavailable (see DebuggeeCallService.construct) rather than guessing.
	Construction is explicitly experimental.
	=====================================================================
**/
class ConstructorResolver {
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final memory:MemoryReader;

	// hl_alloc_obj is the same at every ONew site. Once a site has supplied it,
	// a site that disagrees is treated as a failed pattern match.
	var allocFnValue:Pointer = Int64.ofInt(0);
	var allocResolved = false;
	final cache:Map<String, Null<ConstructorSite>> = new Map();

	public function new(module:ModuleDebugInfo, jit:JitInfo, memory:MemoryReader) {
		this.module = module;
		this.jit = jit;
		this.memory = memory;
	}

	/**
		The construction recipe for `className`, or null when the program never
		constructs it (no ONew site to mine) or the machine-code pattern is
		absent (a JIT whose output is not recognised).
	**/
	public function resolve(className:String):Null<ConstructorSite> {
		if (cache.exists(className)) {
			return cache.get(className);
		}
		var site = find(className);
		cache.set(className, site);
		return site;
	}

	function find(className:String):Null<ConstructorSite> {
		for (fidx in 0...module.functionCount()) {
			var ops = module.opcodes(fidx);
			var regs = module.registers(fidx);

			for (op in 0...ops.length) {
				var dst = newDstForClass(ops[op], regs, className);
				if (dst < 0) {
					continue;
				}
				var mined = mineOnew(fidx, op);
				if (mined == null) {
					continue; // pattern not found at this site: try the next one
				}
				var ctorOp = ctorCallAfter(ops, op, dst);
				if (ctorOp < 0) {
					continue; // no constructor call follows (unusual): try the next site
				}
				// the OCall carries a raw findex; functionType/functionEntry expect the
				// function ARRAY index, which is a different number
				var ctorIndex = module.callTargetFunction(fidx, ctorOp);
				if (ctorIndex < 0) {
					continue;
				}
				return {typePointer: mined.typePtr, allocFunction: mined.allocFn, constructorIndex: ctorIndex};
			}
		}
		return null;
	}

	// The dst register of an `ONew` whose type is HObj/HStruct named `className`.
	function newDstForClass(opcode:Opcode, regs:Array<HLType>, className:String):Int {
		return switch (opcode) {
			case ONew(dst) if (dst >= 0 && dst < regs.length && isNamed(regs[dst], className)): dst;
			default: -1;
		}
	}

	static function isNamed(t:HLType, className:String):Bool {
		return switch (t) {
			case HObj(p), HStruct(p): p != null && p.name == className;
			default: false;
		}
	}

	// Reads the ONew site's machine code and extracts the class type pointer and
	// the allocator address from its set-argument-then-call sequence.
	function mineOnew(fidx:Int, op:Int):Null<{typePtr:Pointer, allocFn:Pointer}> {
		var start = jit.addressOf(fidx, op);
		var end = jit.addressOf(fidx, op + 1);
		var len = Int64.toInt(Int64.sub(end, start));
		if (len <= 0 || len > MachineCode.MAX_SITE_BYTES) {
			return null; // implausible span
		}
		var code = memory.read(start, len);
		var i = 0;
		while (i < len) {
			var mined = MachineCode.mineArgThenCall(code, i, len, jit.is64, jit.winCall);
			if (mined != null) {
				if (allocResolved && !Int64.eq(mined.fn, allocFnValue)) {
					// this site disagrees with an earlier one on the allocator: reject it
					return null;
				}
				allocFnValue = mined.fn;
				allocResolved = true;
				return {typePtr: mined.arg, allocFn: mined.fn};
			}
			i++;
		}
		return null;
	}

	// The position of the constructor call: the first OCall within 32 opcodes
	// after `op` whose first argument is `dst`, the new instance. -1 if none.
	function ctorCallAfter(ops:Array<Opcode>, op:Int, dst:Int):Int {
		var limit = op + 32 < ops.length ? op + 32 : ops.length;
		for (i in (op + 1)...limit) {
			var isCtorCall = switch (ops[i]) {
				case OCall1(_, _, a0), OCall2(_, _, a0, _), OCall3(_, _, a0, _, _),
					OCall4(_, _, a0, _, _, _): a0 == dst;
				case OCallN(_, _, args): args.length > 0 && args[0] == dst;
				default: false;
			}
			if (isCtorCall) {
				return i;
			}
		}
		return -1;
	}
}

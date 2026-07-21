package debug.eval.call;

import debug.Pointer;
import debug.module.JitInfo;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;

import format.hl.Data.HLType;
import format.hl.Data.Opcode;
import haxe.Int64;

/**
	Resolves everything needed to construct `new SomeClass(args)` in the debuggee.

	================================ HACK ================================
	There is NO clean, supported way to reach HashLink's object allocator
	(`hl_alloc_obj`) or a class's runtime `hl_type*` from the debug handshake:
	the handshake only exposes addresses for *bytecode* functions, and
	`hl_alloc_obj` is a C runtime native with no findex. So we recover them by
	DISASSEMBLING the machine code the JIT emits for an `ONew` opcode.

	Every `new` in Haxe compiles to `ONew dst` (allocate) followed by a call to
	the constructor. For an HOBJ/HSTRUCT, `ONew` is lowered (hashlink `jit.c`,
	`ONew` -> `call_native_consts(hl_alloc_obj, {dst->t}, 1)`) to a fixed
	set-argument-then-call sequence — the class type pointer into arg0, then
	`mov (r/e)ax, <hl_alloc_obj> ; call`:

	```
	x86-64:  48 B9/BF <typePtr:8>   mov rcx/rdi, <class hl_type*>  (win64/SysV arg0)
	         48 B8 <allocFn:8>      mov rax, <hl_alloc_obj>
	         FF D0                  call rax
	x86:     68 <typePtr:4>         push <class hl_type*>          (cdecl arg0)
	         B8 <allocFn:4>         mov eax, <hl_alloc_obj>
	         FF D0                  call eax
	```

	We scan an `ONew` site for that sequence (MachineCode.mineArgThenCall picks
	the arch form) and read the type pointer and allocator address. The
	constructor's findex comes from the `OCall*` that follows, whose first
	argument is the freshly allocated register. A single `ONew SomeClass` site
	therefore yields everything to construct that class — and it only exists when
	the program actually constructs the class, which is exactly the DCE-limited
	scope we accept (a class the program never `new`s cannot be constructed, and
	its constructor may have been stripped anyway).

	This is FRAGILE: it depends on the exact instruction selection of the
	HashLink JIT. If the pattern is ever not found the feature reports itself
	unavailable (see VariableInspector) rather than guessing — construction is
	explicitly experimental.
	=====================================================================
**/
class ConstructorResolver {
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final memory:MemoryReader;
	// hl_alloc_obj is the same across every ONew site; once seen, later sites
	// must agree with it (a sanity check on the pattern match).
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
		constructs it (so we have no ONew site to mine) or the machine-code
		pattern is absent (non-x86-64 / a JIT we don't recognise).
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
					continue; // pattern not found at this site: try another / give up
				}
				var ctorOp = ctorCallAfter(ops, op, dst);
				if (ctorOp < 0) {
					continue; // no constructor call follows (unusual): try another site
				}
				// map the raw findex in the OCall to a function ARRAY index (what
				// functionType/functionEntry expect) — findex != array position
				var ctorIndex = module.callTargetFunction(fidx, ctorOp);
				if (ctorIndex < 0) {
					continue;
				}
				return {typePointer: mined.typePtr, allocFunction: mined.allocFn, ctorFindex: ctorIndex};
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
	// the allocator address from the fixed mov/mov/call sequence.
	function mineOnew(fidx:Int, op:Int):Null<{typePtr:Pointer, allocFn:Pointer}> {
		var start = jit.addressOf(fidx, op);
		var end = jit.addressOf(fidx, op + 1);
		var len = Int64.toInt(Int64.sub(end, start));
		if (len <= 0 || len > MachineCode.MAX_SITE_BYTES) {
			return null; // implausible span
		}
		var code = memory.read(start, len);
		// look for the alloc call: arg0 = type ptr, then mov (r/e)ax,allocFn ; call
		var i = 0;
		while (i < len) {
			var mined = MachineCode.mineArgThenCall(code, i, len, jit.is64, jit.winCall);
			if (mined != null) {
				if (allocResolved && !Int64.eq(mined.fn, allocFnValue)) {
					// two ONew sites disagree on the allocator: don't trust it
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

	// The op POSITION of the first OCall after `op` whose first argument is `dst`
	// (the freshly allocated instance) — the constructor call. -1 if none within
	// a short window.
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

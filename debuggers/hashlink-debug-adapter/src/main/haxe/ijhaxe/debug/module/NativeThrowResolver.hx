package ijhaxe.debug.module;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.eval.call.MachineCode;
import ijhaxe.debug.module.ExceptionSites.ThrowSite;
import ijhaxe.debug.target.MemoryReader;

import haxe.Int64;

/**
	Resolves the runtime address of HashLink's `hl_throw(vdynamic*)` — the single
	C function through which EVERY exception passes, whether a bytecode `OThrow`
	or a runtime error raised inside the VM (null access, array-out-of-bounds,
	invalid cast, division by zero). Trapping `hl_throw` therefore catches the
	runtime errors that have no bytecode throw site and would otherwise escape
	the OThrow-based exception breakpoints (see ExceptionSites).

	The debug handshake exposes addresses for bytecode functions only, so — like
	NativeResolver mining a native from an OCall — we disassemble an `OThrow`
	jitted site: hashlink `jit.c` compiles OThrow to a `call_native(hl_throw)`,
	i.e. the fixed `mov (r/e)ax, <hl_throw> ; call` sequence (MachineCode picks
	the x86-64 or x86 form), so the immediate IS hl_throw's absolute address.

	When no OThrow site exists (a program that never throws) or the pattern is
	absent, resolution fails and the caller degrades gracefully (the VM-exception
	breakpoint simply cannot arm).
**/
class NativeThrowResolver {
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final memory:MemoryReader;
	final sites:ExceptionSites;
	var resolved:Bool = false;
	var cached:Null<Pointer> = null;

	public function new(module:ModuleDebugInfo, jit:JitInfo, memory:MemoryReader, sites:ExceptionSites) {
		this.module = module;
		this.jit = jit;
		this.memory = memory;
		this.sites = sites;
	}

	/**
		hl_throw's runtime address, or null when it can't be mined (cached).
	**/
	public function resolve():Null<Pointer> {
		if (resolved) {
			return cached;
		}
		resolved = true;
		for (site in sites.all()) {
			var addr = mineCallSite(site);
			if (addr != null) {
				cached = addr;
				return cached;
			}
		}
		return null;
	}

	// Reads an OThrow site's machine code and pulls hl_throw's address out of the
	// `mov rax, imm64 ; call rax` the JIT emitted for it (`mov eax, imm32 ;
	// call eax` on a 32-bit VM).
	function mineCallSite(site:ThrowSite):Null<Pointer> {
		var start = jit.addressOf(site.fidx, site.op);
		var end = jit.addressOf(site.fidx, site.op + 1);
		var len = Int64.toInt(Int64.sub(end, start));
		if (len <= 0 || len > 256) {
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

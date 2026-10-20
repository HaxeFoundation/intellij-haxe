package ijhaxe.debug.module;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.eval.call.MachineCode;
import ijhaxe.debug.module.ExceptionSites.ThrowSite;
import ijhaxe.debug.target.MemoryReader;

import haxe.Int64;

/**
	Finds the address of HashLink's `hl_throw(vdynamic*)`, the C function that
	every exception passes through. That includes errors the VM raises itself
	(null access, index out of bounds, invalid cast, division by zero), which
	have no bytecode throw site for ExceptionSites to trap.

	The handshake only gives addresses of bytecode functions, so the address is
	mined, that is, read out of the JIT's machine code, like NativeResolver does
	for other natives. hashlink `jit.c` compiles an `OThrow` into
	`mov (r/e)ax, <hl_throw> ; call`, so the immediate operand of that `mov` is
	the address. MachineCode handles both the x86-64 and the x86 form.

	A program without an OThrow site, or with a different code pattern, yields
	no address; the VM-exception breakpoint then cannot be armed.
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
		The address of hl_throw, or null when no throw site yields it. Cached after the first call.
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

	// Reads the machine code of one throw site and extracts hl_throw's address
	// from its `mov rax, imm64 ; call rax` (`mov eax, imm32 ; call eax` on 32-bit).
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

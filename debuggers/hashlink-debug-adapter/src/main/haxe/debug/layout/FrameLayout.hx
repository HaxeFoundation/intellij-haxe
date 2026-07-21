package debug.layout;

import format.hl.Data.HLType;

/**
	Reconstructs the JIT's per-register stack layout for a function, so a bytecode
	register can be read at `ebp + offset`. HashLink 1.x does not transmit register
	locations, so we replicate the allocator (a port of hld `getFunctionRegs`, which
	mirrors `jit.c`'s prologue).

	Layout: locals and register-passed arguments are placed below the frame base at
	negative offsets (`ebp - size`, accumulated with type size + alignment); stack-
	passed arguments sit above it at `argsSize + ptr*2` (skipping the saved base
	pointer and return address). On Windows x64 every argument is stack-passed; on
	System V (64-bit non-Windows) the first six of each of the integer and float
	classes are passed in registers and spilled into the locals area.
**/
class FrameLayout {
	final align:Align;
	final isWindows:Bool;

	public function new(align:Align, isWindows:Bool) {
		this.align = align;
		this.isWindows = isWindows;
	}

	/**
		Offsets for every register of a function: `regs` are the register types
		(from the bytecode) and `nargs` how many of them are arguments.
	**/
	public function registerOffsets(regs:Array<HLType>, nargs:Int):Array<RegisterSlot> {
		var result:Array<RegisterSlot> = [];
		var argsSize = 0;
		var size = 0;
		var intRegs = 0;
		var floatRegs = 0;

		for (i in 0...nargs) {
			var t = regs[i];
			if (align.is64 && !isWindows) {
				var passedInRegister = align.isFloat(t) ? (++floatRegs <= 6) : (++intRegs <= 6);
				if (passedInRegister) {
					// spilled into the locals area, below the frame base
					size += align.typeSize(t);
					size += align.padSize(size, t);
					result[i] = {t: t, offset: -size};
					continue;
				}
			}
			// stack-passed: above the frame base, past the saved rbp + return address
			result[i] = {t: t, offset: argsSize + align.ptr * 2};
			argsSize += align.stackSize(t);
		}

		for (i in nargs...regs.length) {
			var t = regs[i];
			size += align.typeSize(t);
			size += align.padSize(size, t);
			result[i] = {t: t, offset: -size};
		}

		return result;
	}
}

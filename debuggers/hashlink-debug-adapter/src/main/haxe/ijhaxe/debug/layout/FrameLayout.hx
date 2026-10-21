package ijhaxe.debug.layout;

import format.hl.Data.HLType;

/**
	Computes where the JIT stores each bytecode register of a function, so a
	register can be read at `ebp + offset`. HashLink 1.x does not send register
	locations, so this repeats the JIT's own allocation (a port of hld
	`getFunctionRegs`, which mirrors the `jit.c` prologue).

	Locals and register-passed arguments sit below the frame base, at negative
	offsets that grow by each type's size plus alignment. Stack-passed arguments
	sit above it, starting at `ptr*2` to skip the saved base pointer and the
	return address. The Windows x64 convention passes every argument on the
	stack. System V (64-bit non-Windows) passes the first six integer and the
	first six float arguments in registers, which the prologue spills into the
	locals area.
**/
class FrameLayout {
	final align:Align;
	final usesWindowsAbi:Bool;

	public function new(align:Align, usesWindowsAbi:Bool) {
		this.align = align;
		this.usesWindowsAbi = usesWindowsAbi;
	}

	/**
		The slot of every register of a function: `regs` are the register types
		from the bytecode, and the first `nargs` of them are the arguments.
	**/
	public function registerOffsets(regs:Array<HLType>, nargs:Int):Array<RegisterSlot> {
		var result:Array<RegisterSlot> = [];
		var argsSize = 0;
		var size = 0;
		var intRegs = 0;
		var floatRegs = 0;

		for (i in 0...nargs) {
			var t = regs[i];
			if (align.is64 && !usesWindowsAbi) {
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

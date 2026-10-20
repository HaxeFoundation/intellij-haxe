package ijhaxe.debug.eval.call;
import ijhaxe.debug.DebugError;
import ijhaxe.debug.eval.call.CallArg;

import haxe.Int64;
import haxe.io.Bytes;
import haxe.io.BytesBuffer;

/**
	Emits the x86-64 eval-call trampoline: self-contained machine code that
	calls a function in the debuggee and traps with an INT3 when it returns.
	It ports the assembly in vshaxe/hashlink-debugger `hld/Eval.evalCall`;
	`X86CallEmitter` is the 32-bit counterpart.

	The debug natives can write only Esp, Eip and Rax, so the argument
	registers cannot be set from outside. The trampoline sets them itself:

	1. save the scratch and argument registers,
	2. move each argument into its calling-convention register,
	3. `mov rax, <addr>` and `call rax`,
	4. capture the return value (RAX, or XMM0 copied to RAX for a float),
	5. restore the saved registers and execute `int3`.

	Pure; unit tests pin the exact byte sequences.
**/
class X64CallEmitter implements CallTrampoline {
	// x86-64 register numbers, as encoded in instructions
	static inline var RAX = 0;
	static inline var RCX = 1;
	static inline var RDX = 2;
	static inline var R8 = 8;
	static inline var R9 = 9;
	static inline var R10 = 10;
	static inline var R11 = 11;
	static inline var RSI = 6;
	static inline var RDI = 7;

	final winCall:Bool;
	// Registers saved around the call, as in hld: the argument registers plus
	// the other scratch registers. The interrupted function may hold live
	// values in them.
	final scratch:Array<Int>;

	public function new(winCall:Bool) {
		this.winCall = winCall;
		scratch = winCall ? [RCX, RDX, R8, R9, R10, R11] : [RDI, RSI, RDX, RCX, R8, R9, R10, R11];
	}

	/**
		The trampoline bytes that call `funcAddr` with `args`. A nonzero
		`floatBits` copies XMM0 into RAX after the call; F32 and F64 both sit in
		XMM0's low bits, so both widths take the same path. Throws when an
		argument does not fit in a register.
	**/
	public function build(funcAddr:Int64, args:Array<CallArg>, floatBits:Int):Bytes {
		var out = new BytesBuffer();

		// save each scratch register, plus one XMM register per scratch register from XMM0 up
		for (i in 0...scratch.length) {
			pushCpu(out, scratch[i]);
			pushXmm(out, i);
		}
		// place each argument in its calling-convention register
		var placements = placeArgs(args);
		// load in reverse order, as hld does, so a register used as scratch by
		// one load cannot overwrite an argument loaded before it
		for (i in 0...args.length) {
			var idx = args.length - 1 - i;
			var p = placements[idx];
			if (p.xmm) {
				setXmm(out, p.reg, args[idx].bits);
			} else {
				setCpu(out, p.reg, args[idx].bits);
			}
		}
		// mov rax, funcAddr ; call rax
		setCpu(out, RAX, funcAddr);
		out.addByte(0xFF);
		out.addByte(0xD0);
		if (floatBits != 0) {
			captureXmm0ToRax(out);
		}
		// restore the scratch registers in reverse
		for (i in 0...scratch.length) {
			var idx = scratch.length - 1 - i;
			popXmm(out, idx);
			popCpu(out, scratch[idx]);
		}
		out.addByte(0xCC); // int3
		return out.getBytes();
	}

	// The register each argument goes in.
	// TODO: stack-passed arguments, beyond 4 on win64 and 6 integer / 8 float on SysV.
	function placeArgs(args:Array<CallArg>):Array<ArgRegister> {
		var result:Array<ArgRegister> = [];
		if (winCall) {
			// by position: argument i uses slot i (RCX/RDX/R8/R9 or XMM0..3)
			var cpu = [RCX, RDX, R8, R9];
			for (i in 0...args.length) {
				if (i >= cpu.length) {
					throw new DebugError("Too many arguments to call (max " + cpu.length + ")");
				}
				result.push(args[i].isFloat ? {reg: i, xmm: true} : {reg: cpu[i], xmm: false});
			}
			return result;
		}
		// SysV: independent integer and float sequences
		var intRegs = [RDI, RSI, RDX, RCX, R8, R9];
		var nextInt = 0;
		var nextFloat = 0;
		for (arg in args) {
			if (arg.isFloat) {
				if (nextFloat >= 8) {
					throw new DebugError("Too many float arguments to call");
				}
				result.push({reg: nextFloat++, xmm: true});
			} else {
				if (nextInt >= intRegs.length) {
					throw new DebugError("Too many arguments to call (max " + intRegs.length + ")");
				}
				result.push({reg: intRegs[nextInt++], xmm: false});
			}
		}
		return result;
	}

	// --- instruction encoders (verified against hld's byte sequences) ---

	static function pushCpu(out:BytesBuffer, reg:Int):Void {
		if (reg >= 8) {
			out.addByte(0x41); // REX.B
		}
		out.addByte(0x50 + (reg & 7));
	}

	static function popCpu(out:BytesBuffer, reg:Int):Void {
		if (reg >= 8) {
			out.addByte(0x41);
		}
		out.addByte(0x58 + (reg & 7));
	}

	// sub rsp,16 ; movsd [rsp], xmm<reg>
	static function pushXmm(out:BytesBuffer, reg:Int):Void {
		subRsp16(out);
		out.addByte(0xF2);
		out.addByte(0x0F);
		out.addByte(0x11);
		out.addByte(0x04 + (reg & 7) * 8);
		out.addByte(0x24);
	}

	// movsd xmm<reg>, [rsp] ; add rsp,16
	static function popXmm(out:BytesBuffer, reg:Int):Void {
		out.addByte(0xF2);
		out.addByte(0x0F);
		out.addByte(0x10);
		out.addByte(0x04 + (reg & 7) * 8);
		out.addByte(0x24);
		addRsp16(out);
	}

	// mov r64, imm64
	static function setCpu(out:BytesBuffer, reg:Int, value:Int64):Void {
		out.addByte(0x48 | (reg >= 8 ? 1 : 0)); // REX.W (+ REX.B for r8-15)
		out.addByte(0xB8 + (reg & 7));
		addInt64(out, value);
	}

	/**
		A stub that loads XMM0 with `bits` and traps, for the linux
		float-register write workaround. hl's linux debug natives cannot WRITE
		float registers: their ptrace write path does not handle the FP
		pseudo-offsets their read path defines. They can inject code, as every
		eval-call does, so the register write runs as a small injected stub. The
		stub clobbers RAX; the injector saves and restores RAX around every
		injected run.
	**/
	public static function buildXmm0Load(bits:Int64):Bytes {
		var out = new BytesBuffer();
		setXmm(out, 0, bits);
		out.addByte(0xCC); // the trailing INT3 the injector runs to
		return out.getBytes();
	}

	// Loads an XMM register through RAX and an 8-byte stack slot. `push rax`
	// moves RSP by 8, so the slot is released with `add rsp,8`, not popXmm's
	// `add rsp,16`. An unbalanced stack corrupts the restore of the scratch
	// registers.
	static function setXmm(out:BytesBuffer, reg:Int, value:Int64):Void {
		setCpu(out, RAX, value);
		pushCpu(out, RAX); // rsp -= 8
		// movsd xmm<reg>, [rsp]
		out.addByte(0xF2);
		out.addByte(0x0F);
		out.addByte(0x10);
		out.addByte(0x04 + (reg & 7) * 8);
		out.addByte(0x24);
		// add rsp, 8 (balance the push)
		out.addByte(0x48);
		out.addByte(0x83);
		out.addByte(0xC4);
		out.addByte(0x08);
	}

	// sub rsp,16 ; movsd [rsp], xmm0 ; pop rax ; add rsp,8
	static function captureXmm0ToRax(out:BytesBuffer):Void {
		subRsp16(out);
		out.addByte(0xF2);
		out.addByte(0x0F);
		out.addByte(0x11);
		out.addByte(0x04);
		out.addByte(0x24);
		popCpu(out, RAX);
		// add rsp, 8
		out.addByte(0x48);
		out.addByte(0x83);
		out.addByte(0xC4);
		out.addByte(0x08);
	}

	static inline function subRsp16(out:BytesBuffer):Void {
		out.addByte(0x48);
		out.addByte(0x83);
		out.addByte(0xEC);
		out.addByte(0x10);
	}

	static inline function addRsp16(out:BytesBuffer):Void {
		out.addByte(0x48);
		out.addByte(0x83);
		out.addByte(0xC4);
		out.addByte(0x10);
	}

	static function addInt64(out:BytesBuffer, value:Int64):Void {
		var low = value.low;
		var high = value.high;
		for (i in 0...4) {
			out.addByte((low >>> (i * 8)) & 0xFF);
		}
		for (i in 0...4) {
			out.addByte((high >>> (i * 8)) & 0xFF);
		}
	}
}

/**
	Where an argument goes: the register number, and whether it is an XMM (float) register.
**/
typedef ArgRegister = {reg:Int, xmm:Bool}

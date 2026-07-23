package debug.eval.call;
import debug.DebugError;
import debug.eval.call.CallArg;

import haxe.Int64;
import haxe.io.Bytes;
import haxe.io.BytesBuffer;

/**
	Emits a self-contained x86-64 machine-code trampoline that calls a function
	in the debuggee and traps (INT3) on return — a port of the assembly in
	vshaxe/hashlink-debugger `hld/Eval.evalCall` (the 32-bit cdecl counterpart is
	`X86CallEmitter`). The trampoline is written OVER the code at the
	stopped thread's instruction pointer (which is guaranteed executable); the
	caller saves and restores the original bytes.

	We cannot write the argument registers from outside (the debug native only
	exposes Esp/Eip/Rax), so the trampoline loads them itself: it saves the
	scratch/argument registers, moves each argument into its calling-convention
	register, `mov rax, <addr>` / `call rax`, captures the return (RAX, or XMM0
	copied to RAX for a float return), restores the saved registers, and `int3`.

	Pure and unit-tested against exact byte sequences.
**/
class X64CallEmitter implements CallTrampoline {
	// x86-64 register encodings (hardware numbers).
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
	// Scratch registers saved around the call (a superset of the argument
	// registers), matching hld: they may hold live values mid-function.
	final scratch:Array<Int>;

	public function new(winCall:Bool) {
		this.winCall = winCall;
		scratch = winCall ? [RCX, RDX, R8, R9, R10, R11] : [RDI, RSI, RDX, RCX, R8, R9, R10, R11];
	}

	/**
		The number of arguments the register-only calling path supports.
	**/
	public function maxArgs():Int {
		return winCall ? 4 : 6; // win64: 4 positional; SysV: 6 int / (8 float, capped here)
	}

	/**
		The trampoline bytes for calling `funcAddr` with `args` (already lowered
		to raw 64-bit register values), capturing a float return through XMM0
		when `floatBits` is nonzero (both widths sit in XMM0's low bits, so 32 and
		64 are handled identically here). Throws when an argument cannot be placed
		in a register (no stack-argument support yet).
	**/
	public function build(funcAddr:Int64, args:Array<CallArg>, floatBits:Int):Bytes {
		var out = new BytesBuffer();
		// save the scratch registers (both the integer reg and its XMM peer)
		for (i in 0...scratch.length) {
			pushCpu(out, scratch[i]);
			pushXmm(out, i);
		}
		// place each argument in its calling-convention register
		var placements = placeArgs(args);
		// load in reverse so an argument register used as scratch for an
		// earlier load is not clobbered (matches hld)
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

	// Which register each argument goes in.
	function placeArgs(args:Array<CallArg>):Array<ArgRegister> {
		var result:Array<ArgRegister> = [];
		if (winCall) {
			// positional: argument i uses slot i (RCX/RDX/R8/R9 or XMM0..3)
			var cpu = [RCX, RDX, R8, R9];
			for (i in 0...args.length) {
				if (i >= 4) {
					throw new DebugError("Too many arguments to call (max " + maxArgs() + ")");
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
					throw new DebugError("Too many arguments to call (max " + maxArgs() + ")");
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
		A minimal stub for the linux float-register-write workaround: loads
		Xmm0 with `bits` and traps. hl's linux debug natives cannot WRITE
		float registers (the ptrace write path never handled the FP
		pseudo-offsets its own read path defines), but they can inject code —
		which is how eval-calls already run — so the register write becomes a
		two-instruction injected stub. RAX is clobbered here; the injector
		saves and restores it around every injected run.
	**/
	public static function buildXmm0Load(bits:Int64):Bytes {
		var out = new BytesBuffer();
		setXmm(out, 0, bits);
		out.addByte(0xCC); // the trailing INT3 the injector runs to
		return out.getBytes();
	}

	// load an XMM register: stage the value through RAX and an 8-byte stack
	// slot. NOTE: `push rax` pushes 8 bytes, so this pops exactly 8 (add rsp,8)
	// — NOT popXmm's `add rsp,16`, which would leave the stack unbalanced and
	// corrupt the scratch-register restore for every float argument.
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
	Where an argument goes: the target register, and whether it's an XMM (float) register.
**/
typedef ArgRegister = {reg:Int, xmm:Bool}

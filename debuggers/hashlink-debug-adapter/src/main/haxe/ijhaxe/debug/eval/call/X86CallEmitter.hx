package ijhaxe.debug.eval.call;

import ijhaxe.debug.eval.call.CallArg;

import haxe.Int64;
import haxe.io.Bytes;
import haxe.io.BytesBuffer;

/**
	Emits a 32-bit (x86) call trampoline, the cdecl counterpart of the x86-64
	`X64CallEmitter`. HashLink's JIT calls natives cdecl on 32-bit: arguments
	pushed right-to-left, caller cleans the stack, int/pointer returned in EAX,
	float/double returned on the x87 stack (ST0).

	The caller sets ESP to a scratch top S below the interrupted frame before
	running this. Layout the trampoline builds:

	```
	push ecx ; push edx          save the caller-saved scratch registers
	push <args, right-to-left>   each dword; an 8-byte double is two pushes
	mov eax, funcAddr ; call eax
	add esp, argBytes            cdecl caller cleanup -> ESP back to S-8
	[float] fstp {qword|dword} [esp+8]   store ST0 into the return slot at [S]
	pop edx ; pop ecx            restore the scratch registers
	int3
	```

	The result: an int/pointer is in EAX (the caller reads it directly); a float
	is written to [S] — a free 8-byte slot just below the interrupted frame — and
	the caller reads it from there (ST0 is not exposed by HL's debug register
	API, so it cannot be read after the trap; it must be spilled here). `wide`
	float args (HF64) push two dwords; F32 return spills a dword, F64 a qword.

	Pure and unit-tested against exact byte sequences.
**/
class X86CallEmitter implements CallTrampoline {
	static inline var MAX_ARGS = 16; // cdecl is stack-based; a sane cap vs the scratch stack

	public function new() {}

	public function maxArgs():Int {
		return MAX_ARGS;
	}

	public function build(funcAddr:Int64, args:Array<CallArg>, floatBits:Int):Bytes {
		if (args.length > MAX_ARGS) {
			throw new ijhaxe.debug.DebugError("Too many arguments to call (max " + MAX_ARGS + ")");
		}
		var out = new BytesBuffer();
		out.addByte(0x51); // push ecx
		out.addByte(0x52); // push edx

		// arguments right-to-left; within a wide (8-byte) arg push high then low
		// so the low dword lands at the lower address (little-endian double)
		var argBytes = 0;
		for (i in 0...args.length) {
			var arg = args[args.length - 1 - i];
			if (arg.wide == true) {
				pushImm32(out, arg.bits.high);
				pushImm32(out, arg.bits.low);
				argBytes += 8;
			} else {
				pushImm32(out, arg.bits.low);
				argBytes += 4;
			}
		}

		out.addByte(0xB8); // mov eax, imm32
		addInt32(out, funcAddr.low);
		out.addByte(0xFF); // call eax
		out.addByte(0xD0);

		if (argBytes > 0) {
			out.addByte(0x81); // add esp, imm32
			out.addByte(0xC4);
			addInt32(out, argBytes);
		}

		if (floatBits != 0) {
			// spill ST0 into the return slot at [S] (== [esp+8], above the saved
			// ecx/edx): qword for a double, dword for a single
			out.addByte(floatBits == 64 ? 0xDD : 0xD9); // fstp m64 / m32
			out.addByte(0x5C); // modrm: [esp+disp8], /3
			out.addByte(0x24); // sib: base=esp
			out.addByte(0x08); // disp8 = +8
		}

		out.addByte(0x5A); // pop edx
		out.addByte(0x59); // pop ecx
		out.addByte(0xCC); // int3
		return out.getBytes();
	}

	static function pushImm32(out:BytesBuffer, value:Int):Void {
		out.addByte(0x68); // push imm32
		addInt32(out, value);
	}

	static function addInt32(out:BytesBuffer, value:Int):Void {
		for (i in 0...4) {
			out.addByte((value >>> (i * 8)) & 0xFF);
		}
	}
}

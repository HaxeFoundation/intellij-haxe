package ijhaxe.debug.eval.call;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.eval.call.CallArg;

import haxe.Int64;
import haxe.io.Bytes;
import haxe.io.BytesBuffer;

/**
	Emits the 32-bit (x86) eval-call trampoline, the cdecl counterpart of
	`X64CallEmitter`. HashLink's 32-bit JIT calls natives with cdecl:
	arguments are pushed right to left, the caller cleans the stack, an int or
	pointer returns in EAX, and a float returns on the x87 stack (ST0).

	Before running it, the caller points ESP at a scratch stack top S below the
	interrupted frame. The trampoline is:

	```
	push ecx ; push edx          save the caller-saved scratch registers
	push <args, right-to-left>   each dword; an 8-byte double is two pushes
	mov eax, funcAddr ; call eax
	add esp, argBytes            cdecl caller cleanup -> ESP back to S-8
	[float] fstp {qword|dword} [esp+8]   store ST0 into the return slot at [S]
	pop edx ; pop ecx            restore the scratch registers
	int3
	```

	An int or pointer result stays in EAX, where the caller reads it. A float
	result is stored at [S], the top of the scratch stack, and the caller reads
	it from there: HL's debug register API does not expose ST0, so the value
	must be stored before the trap. An F32 result is stored as a dword, an F64
	as a qword, and a `wide` (HF64) argument takes two pushes.

	Pure; unit tests pin the exact byte sequences.
**/
class X86CallEmitter implements CallTrampoline {
	// cdecl passes every argument on the stack; the cap keeps them well inside the scratch stack
	static inline var MAX_ARGS = 16;

	public function new() {}

	public function build(funcAddr:Int64, args:Array<CallArg>, floatBits:Int):Bytes {
		if (args.length > MAX_ARGS) {
			throw new DebugError("Too many arguments to call (max " + MAX_ARGS + ")");
		}
		var out = new BytesBuffer();
		out.addByte(0x51); // push ecx
		out.addByte(0x52); // push edx

		// arguments right to left; a wide (8-byte) argument pushes its high dword
		// first, so the low dword lands at the lower address of the little-endian double
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
			// store ST0 at [S], which is [esp+8] above the saved ecx/edx: a qword
			// for a double, a dword for a single
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

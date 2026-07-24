package ijhaxe.debug.eval.call;

import ijhaxe.debug.eval.call.CallArg;

import haxe.Int64;
import haxe.io.Bytes;

/**
	A machine-code trampoline that calls a function in the debuggee and traps
	(INT3) on return. Selected by CPU architecture — `X64CallEmitter` emits
	x86-64, `X86CallEmitter` emits 32-bit cdecl — so the rest of the
	adapter drives eval-calls without knowing the bitness.

	The trampoline is written OVER the code at the stopped thread's instruction
	pointer (guaranteed executable); the caller saves/restores the original
	bytes and the Eip/Esp/Rax registers.
**/
interface CallTrampoline {
	function maxArgs():Int;

	/**
		Trampoline bytes calling `funcAddr` with `args` (already lowered to raw
		register/stack values). `floatBits` is the return's float width: 0 for an
		int/pointer return (delivered in RAX/EAX), 32 or 64 for a float return.
		A 64-bit float return's bits are delivered through RAX (x86-64) or a
		caller-known scratch slot (x86 — see X86CallEmitter).
	**/
	function build(funcAddr:Int64, args:Array<CallArg>, floatBits:Int):Bytes;
}

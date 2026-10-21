package ijhaxe.debug.eval.call;

import ijhaxe.debug.eval.call.CallArg;

import haxe.Int64;
import haxe.io.Bytes;

/**
	Builds the machine code for an eval-call, which runs a debuggee function
	on behalf of an evaluate expression. The code is a trampoline: it calls the
	function and then traps with an INT3. `X64CallEmitter` emits x86-64 code
	and `X86CallEmitter` 32-bit cdecl code, so the rest of the adapter runs
	eval-calls without knowing the bitness.

	The trampoline is written OVER the code at the stopped thread's
	instruction pointer, which is guaranteed to be executable. The caller
	saves and restores the original bytes and the Eip/Esp/Rax registers.
**/
interface CallTrampoline {
	/**
		The trampoline bytes that call `funcAddr` with `args`. `floatBits` is the
		width of a float return: 0 for an int or pointer return (delivered in
		RAX/EAX), 32 or 64 for a float. On x86-64 a float result is copied into
		RAX; on x86 it is stored in a scratch slot the caller knows (see
		X86CallEmitter). Throws when the arguments exceed what the calling
		convention's register or stack path carries.
	**/
	function build(funcAddr:Int64, args:Array<CallArg>, floatBits:Int):Bytes;
}

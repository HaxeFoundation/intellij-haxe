package ijhaxe.debug.eval.call;

import haxe.Int64;

/**
	A call argument, lowered to the raw 64-bit value that goes in its register
	(x86-64) or is pushed on the stack (x86). `wide` marks an 8-byte value (an
	HF64 double) so the x86 trampoline pushes two dwords rather than one; the
	x86-64 trampoline ignores it (every argument occupies one register slot).

	Architecture-neutral, shared by CallTrampoline and both emitters.
**/
typedef CallArg = {
	var isFloat:Bool;
	var bits:Int64;
	var ?wide:Bool;
}

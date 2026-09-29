package ijhaxe.debug.eval.call;

import haxe.Int64;

/**
	A call argument as raw bits: the 64-bit value that goes into its register
	(x86-64) or onto the stack (x86). `wide` marks an 8-byte value (an HF64
	double), which the x86 trampoline pushes as two dwords. The x86-64
	trampoline ignores it, because every argument fills one register.
**/
typedef CallArg = {
	var isFloat:Bool;
	var bits:Int64;
	var ?wide:Bool;
}

package ijhaxe.debug.module;

/**
	Where the JIT placed one function, from the handshake. `start` is the byte
	offset of its machine code from `JitInfo.jitCodeBase`. `offsets[op]` is the
	byte offset of opcode `op` from the function start, and `offsets[nops]` is
	the end of the function. `large` records whether the handshake sent the
	offsets as int32 rather than uint16.
**/
typedef JitFunction = {
	var nops:Int;
	var start:Int;
	var large:Bool;
	var offsets:Array<Int>;
}

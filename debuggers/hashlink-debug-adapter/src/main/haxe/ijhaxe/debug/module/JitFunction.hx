package ijhaxe.debug.module;

/**
	One function's JIT layout from the handshake: `start` is the byte offset of
	its machine code from `JitInfo.jitCodeBase`; `offsets[op]` is the byte offset
	of opcode `op` from the function start; `offsets[nops]` marks the end.
**/
typedef JitFunction = {
	var nops:Int;
	var start:Int;
	var large:Bool;
	var offsets:Array<Int>;
}

package ijhaxe.debug.eval.call;

import ijhaxe.debug.Pointer;

/**
	The pieces recovered from an `ONew SomeClass` machine-code site, enough to
	construct the class in the debuggee: the class's runtime `hl_type*`, the
	allocator (`hl_alloc_obj`) address, and the constructor's bytecode findex.
	See ConstructorResolver — all of this is mined by disassembly (a hack).
**/
typedef ConstructorSite = {
	var typePointer:Pointer;
	var allocFunction:Pointer;
	var ctorFindex:Int;
}

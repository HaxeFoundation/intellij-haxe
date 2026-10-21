package ijhaxe.debug.eval.call;

import ijhaxe.debug.Pointer;

/**
	What an `ONew SomeClass` machine-code site yields, enough to construct the
	class in the debuggee: the class's runtime `hl_type*`, the address of the
	allocator (`hl_alloc_obj`), and the constructor. ConstructorResolver finds
	all of it by disassembly (a hack).
**/
typedef ConstructorSite = {
	var typePointer:Pointer;
	var allocFunction:Pointer;
	// the constructor's index in the module's function array, not its findex
	var constructorIndex:Int;
}

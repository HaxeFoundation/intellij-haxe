package debug.values;

import debug.Pointer;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

/**
 * What a DAP `variablesReference` resolves to. References are handed out per stop
 * and invalidated on resume.
 */
enum RefTarget {
	RefLocals(frameId:Int);
	RefObject(pointer:Pointer, type:HLType);
	// A class's statics singleton; decoded like an object but function-typed fields
	// (the static methods, which share the container) are hidden.
	RefStatics(pointer:Pointer, proto:ObjPrototype);
}

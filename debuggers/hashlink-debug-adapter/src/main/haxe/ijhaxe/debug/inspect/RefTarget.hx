package ijhaxe.debug.inspect;

import ijhaxe.debug.Pointer;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

/**
	What a DAP `variablesReference` resolves to. References are handed out per stop
	and invalidated on resume.
**/
enum RefTarget {
	RefLocals(frameId:Int);
	RefObject(pointer:Pointer, type:HLType);
	// A class's statics singleton; decoded like an object but function-typed fields
	// (the static methods, which share the container) are hidden.
	RefStatics(pointer:Pointer, proto:ObjPrototype);
	// The frame's HL bytecode registers (r0..rN), plus the thread's CPU
	// registers on the top frame.
	RefRegisters(frameId:Int);
}

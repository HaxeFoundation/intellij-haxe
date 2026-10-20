package ijhaxe.debug.eval;

import ijhaxe.debug.Pointer;
import format.hl.Data.HLType;
import haxe.Int64;

/**
	A value in the expression interpreter. Reads of debuggee memory produce
	these, operators combine them inside the adapter, and the consumers (call
	arguments, assignments, display) convert them back.
**/
enum EvalValue {
	/**
		An integer, carried as 64 bits so I64 slots fit. HL's own Int is 32-bit.
	**/
	VInt(v:Int64);
	VFloat(v:Float);
	VBool(v:Bool);

	/**
		String CONTENT, held in the adapter. `ptr` is the debuggee String the
		value was read from, so it can be passed on without creating a new one.
		It is null for literals and concatenation results; those are created in
		the debuggee on demand (DebuggeeCallService.makeString).
	**/
	VString(v:String, ptr:Null<Pointer>);
	VNull;

	/**
		A pointer to a value in the debuggee (object, array, map, closure, ...).
	**/
	VObject(raw:Pointer, type:HLType);
}

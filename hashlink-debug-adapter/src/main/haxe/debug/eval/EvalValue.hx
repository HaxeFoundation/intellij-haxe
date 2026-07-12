package debug.eval;

import haxe.Int64;

/**
 * The expression interpreter's typed currency (M21b). Leaves read debuggee
 * memory into one of these; operators fold them adapter-side; sinks (call
 * arguments, assignments, display) convert them back out.
 */
enum EvalValue {
	/** Integer (HL ints are 32-bit; 64-bit carried for I64 slots and range). */
	VInt(v:Int64);
	VFloat(v:Float);
	VBool(v:Bool);
	/**
	 * String CONTENT, adapter-side. `ptr` is the debuggee String when the value
	 * came from one (pass-through without re-materializing); null for literals
	 * and concat results (materialized on demand via makeString).
	 */
	VString(v:String, ptr:Null<debug.Pointer>);
	VNull;
	/** A debuggee-resident pointer value (object/array/map/closure/...). */
	VObject(raw:debug.Pointer, type:format.hl.Data.HLType);
}

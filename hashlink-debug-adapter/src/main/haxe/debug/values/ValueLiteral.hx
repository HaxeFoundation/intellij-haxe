package debug.values;

import haxe.Int64;

/**
 * A concrete value handed to `ValueWriter`: a literal for every primitive,
 * `null`, a string literal (materialized in the debuggee via the eval-call
 * machinery — M13c), or a variable path whose existing value is copied (a
 * pointer copy for reference types). Produced from evaluated expression values
 * by `VariableInspector.writeValue`.
 */
enum ValueLiteral {
	LInt(value:Int64);
	LFloat(value:Float);
	LBool(value:Bool);
	LNull;
	LString(value:String);
	LPath(path:ValuePath);
}

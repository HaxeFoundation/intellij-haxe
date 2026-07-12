package debug.values;

import haxe.Int64;

/**
 * The right-hand side of a value assignment, parsed: literals for every
 * primitive, `null`, a string literal (materialized in the debuggee via the
 * eval-call machinery — M13c), and a variable path whose existing value is
 * copied (a pointer copy for reference types). Parsed by ValueLiteralParser.
 */
enum ValueLiteral {
	LInt(value:Int64);
	LFloat(value:Float);
	LBool(value:Bool);
	LNull;
	LString(value:String);
	LPath(path:ValuePath);
}

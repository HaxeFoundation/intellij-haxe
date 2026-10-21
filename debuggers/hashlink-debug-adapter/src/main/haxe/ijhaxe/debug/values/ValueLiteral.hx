package ijhaxe.debug.values;

import haxe.Int64;

/**
	A value for `ValueWriter.write`: a primitive literal or `null`.
	`VariableMutator.writeValue` produces it from an evaluated expression.
**/
enum ValueLiteral {
	LInt(value:Int64);
	LFloat(value:Float);
	LBool(value:Bool);
	LNull;
}

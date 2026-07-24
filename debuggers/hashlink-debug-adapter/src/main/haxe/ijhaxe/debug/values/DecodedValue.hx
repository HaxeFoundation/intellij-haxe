package ijhaxe.debug.values;
import ijhaxe.dap.protocol.Variable;

/**
	A decoded value ready for a DAP `Variable`: a display string, a type label, and
	a `reference` (0 = leaf; >0 = an expandable value the client can request the
	children of).
**/
typedef DecodedValue = {
	var value:String;
	var type:String;
	var reference:Int;
}

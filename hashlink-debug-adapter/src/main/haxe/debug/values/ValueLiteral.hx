package debug.values;

import haxe.Int64;

/**
 * The right-hand side of a value assignment, parsed. The writable subset is
 * deliberately allocation-free: literals for every primitive, `null`, and a
 * variable path whose existing value is copied (a pointer copy for reference
 * types). Creating NEW heap values (strings, objects) requires calling the
 * debuggee's allocator and is out of scope until the eval-call machinery
 * exists. Parsed by ValueLiteralParser.
 */
enum ValueLiteral {
	LInt(value:Int64);
	LFloat(value:Float);
	LBool(value:Bool);
	LNull;
	LPath(path:ValuePath);
}

package ijhaxe.debug.values;

/**
	The key layout variant of a native HashLink map, matching the wrapper class
	(haxe.ds.StringMap / IntMap / ObjectMap).
**/
enum MapKeyKind {
	StringKey; // UCS-2 bytes pointer, stored alongside the value
	IntKey; // i32, stored in the entries array
	Int64Key; // i64, stored in the entries array (hl.types.Int64Map)
	ObjectKey; // dynamic pointer, stored alongside the value
}

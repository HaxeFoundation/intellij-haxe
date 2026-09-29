package ijhaxe.debug.values;

/**
	How a native HashLink map stores its keys, one kind per map class
	(haxe.ds.StringMap, IntMap, ObjectMap, hl.types.Int64Map).
**/
enum MapKeyKind {
	StringKey; // a pointer to UCS-2 characters, stored next to the value
	IntKey; // an i32, stored in the entries array
	Int64Key; // an i64, stored in the entries array
	ObjectKey; // a Dynamic pointer, stored next to the value
}

package ijhaxe.debug.values;

/**
	One step of a parsed variable path: a named field or an integer index.
**/
enum PathAccessor {
	Field(name:String);
	Index(index:Int);
}

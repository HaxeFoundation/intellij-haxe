package ijhaxe.debug.values;

/**
	A variable path such as `obj.items[3].name`: a root name followed by field
	and index accessors. The evaluator resolves it to a slot in debuggee memory.
**/
class ValuePath {
	public final root:String;
	public final accessors:Array<PathAccessor>;

	public function new(root:String, accessors:Array<PathAccessor>) {
		this.root = root;
		this.accessors = accessors;
	}

	/**
		The path as text for display and error messages (`obj.field[3]`).
	**/
	public function display():String {
		var s = root;
		for (a in accessors) {
			s += switch (a) {
				case Field(n): "." + n;
				case Index(i): "[" + i + "]";
			}
		}
		return s;
	}

	/**
		A copy of this path with one more field accessor appended.
	**/
	public function plus(field:String):ValuePath {
		return new ValuePath(root, accessors.concat([Field(field)]));
	}
}

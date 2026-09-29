class Container<T> {
	var items:Array<T>;
	var index:Map<String, Array<Int>>;
	var flags:Array<Bool>;

	public function new(items:Array<T>) {
		this.items = items;
		this.index = new Map<String, Array<Int>>();
	}
}

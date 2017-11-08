abstract Vector<T>(IVector<T>) {
	@:arrayAccess public inline function set (index:Int, value:T):T {
		return this.set (index, value);
	}
}
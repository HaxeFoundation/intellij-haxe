/* Elided/edited version of Map from Haxe Toolkit 3.4.7 */

@:multiType(@:followWithAbstracts K)
abstract Map<K,V>(IMap<K,V> ) {
	public function new();
	public inline function set(key:K, value:V) this.set(key, value);
	@:arrayAccess public inline function get(key:K) return this.get(key);
	public inline function exists(key:K) return this.exists(key);
	public inline function remove(key:K) return this.remove(key);
	public inline function keys():Iterator<K> { Return this.keys();	}
	public inline function iterator():Iterator<V> { return this.iterator();	}
	public inline function toString():String { return this.toString(); }
	@:arrayAccess @:noCompletion public inline function arrayWrite(k:K, v:V):V {
		this.set(k, v);
		return v;
	}

}

@:dox(hide)
@:deprecated
typedef IMap<K, V> = constrainedIMap<K, V>;


interface constrainedIMap<K,V> {
	public function get(k:K):Null<V>;
	public function set(k:K, v:V):Void;
	public function exists(k:K):Bool;
	public function remove(k:K):Bool;
	public function keys():Iterator<K>;
	public function iterator():Iterator<V>;
	public function toString():String;
}





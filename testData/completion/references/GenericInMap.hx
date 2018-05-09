class GenericInMap<T:String> {
  public static function test() {
    var map:IMap<Int, T>;
    for (item in map) {
      item.<caret>
    }
  }
}

interface IMap<K,V> {
  public function get(k:K):Null<V>;
  public function set(k:K, v:V):Void;
  public function exists(k:K):Bool;
  public function remove(k:K):Bool;
  public function keys():Iterator<K>;
  public function iterator():Iterator<V>;
  public function toString():String;
}

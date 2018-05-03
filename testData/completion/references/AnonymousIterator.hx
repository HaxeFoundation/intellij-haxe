interface IMap<K,V> {
  public function get(k:K):Null<V>;
  public function set(k:K, v:V):Void;
  public function exists(k:K):Bool;
  public function remove(k:K):Bool;
  public function keys():Iterator<K>;
  public function iterator():Iterator<V>;
  public function toString():String;
}

class AnonymousIterator {
  public static function new() {
    var map:IMap<String, {x:String, y:Int}>;
    for (value in map) {
      value.<caret>
    }
  }
}
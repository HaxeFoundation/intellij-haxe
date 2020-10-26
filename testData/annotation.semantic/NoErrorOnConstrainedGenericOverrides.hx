package;

import Map;

class ClassMap<K:Class<Dynamic>, V> implements IMap<K,V> {
  public function keys():Iterator<K> {}
  public function keyValueIterator():KeyValueIterator<K,V> {}

  public function get(k:K):Null<V> {}
  public function set(k:K, v:V):Void {}
  public function exists(k:K):Bool {}
  public function remove(k:K):Bool {}
  public function iterator():Iterator<V> {}
  public function copy():IMap<K, V> {}
  public function toString():String {}
  public function clear():Void {}
}
package;

import Map;

class ClassMap<K:Class<Dynamic>, V> implements IMap<K,V> {
  public function keys():Iterator<K> {}
  public function keyValueIterator():KeyValueIterator<K,V> {}

// The following must (correctly) be included for there to be no errors in IDEA 2020.1.
// Pre 2020.1 code didn't notice the missing functions.
//  public function get(k:K):Null<V> {}
//  public function set(k:K, v:V):Void {}
//  public function exists(k:K):Bool {}
//  public function remove(k:K):Bool {}
//  public function iterator():Iterator<V> {}
//  public function copy():IMap<K, V> {}
//  public function toString():String {}
//  public function clear():Void {}
}
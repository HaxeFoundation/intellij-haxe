package;

import Map;

class ClassMap<K:Class<Dynamic>, V> implements IMap<K,V> {
  public function keys():Iterator<K> {return null;}
  public function keyValueIterator():KeyValueIterator<K,V> {return null;}

  public function get(k:K):Null<V> {return null;}
  public function set(k:K, v:V):Void {}
  public function exists(k:K):Bool {return false;}
  public function remove(k:K):Bool {return false;}
  public function iterator():Iterator<V> {return null;}
  public function copy():IMap<K, V> {return null;}
  public function toString():String {return null;}
  public function clear():Void {}
}
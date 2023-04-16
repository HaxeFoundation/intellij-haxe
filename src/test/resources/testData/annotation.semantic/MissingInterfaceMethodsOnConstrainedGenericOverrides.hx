package;

import Map;

class ClassMap<K:Class<Dynamic>, V> implements <error descr="Not implemented methods: iterator, set, get, clear, exists, toString, copy, remove">IMap<K,V></error> {
  public function keys():Iterator<K> {}
  public function keyValueIterator():KeyValueIterator<K,V> {}
}
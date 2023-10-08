package;

import Map;

class ClassMap<K:Class<Dynamic>, V> implements <error descr="Not implemented methods: get, set, exists, remove, iterator, copy, toString, clear" textAttributesKey="ERRORS_ATTRIBUTES">IMap<K,V></error> {
  public function keys():Iterator<K> {}
  public function keyValueIterator():KeyValueIterator<K,V> {}
}
package;

import Map;

class ClassMap<K/*:Class<Dynamic>*/, V> implements IMap<K,V> {
  public function keys():Iterator<K> {}
  public function keyValueIterator():KeyValueIterator<K,V> {}
}
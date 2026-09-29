class ReversedMapValue {
  function main() {
    var reversed:ReversedMap<MyEnum, String> = ["a" => <caret>];
  }
}

// generic order deliberately value-first: only the setter can tell key from value
abstract ReversedMap<V, K>(Map<K, V>) {
  @:arrayAccess public inline function get(k:K):V return this.get(k);
  @:arrayAccess public inline function set(k:K, v:V):V { this.set(k, v); return v; }
}

@:coreType abstract Void { }
@:coreType @:notNull @:runtimeValue abstract Float { }
@:coreType @:notNull @:runtimeValue abstract Int to Float { }

#if (java || cs)
@:coreType @:notNull @:runtimeValue abstract Single to Float from Float {}
#end

typedef Null<T> = T

@:coreType @:notNull @:runtimeValue abstract Bool {
}

@:coreType @:runtimeValue abstract Dynamic<T> {
}

typedef Iterator<T> = {
  function hasNext() : Bool;
  function next() : T;

}
typedef Iterable<T> = {
  function iterator() : Iterator<T>;
}
extern interface ArrayAccess<T> { }
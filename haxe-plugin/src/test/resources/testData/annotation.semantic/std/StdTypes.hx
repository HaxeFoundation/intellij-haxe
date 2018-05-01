extern enum Void { }
extern class Float { }
extern class Int extends Float { }
typedef Null<T> = T
extern enum Bool {
  true;
  false;
}
extern class Dynamic<T> { }
typedef Iterator<T> = {
  function hasNext() : Bool;
  function next() : T;
}
typedef Iterable<T> = {
  function iterator() : Iterator<T>;
}
extern interface ArrayAccess<T> { }

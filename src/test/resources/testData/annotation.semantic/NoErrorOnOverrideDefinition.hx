package;

interface Cloneable<T> {
  function clone():T;
}

class E implements Cloneable<E> {
  public function new() {}
  public function clone(): E {    // Not compatible return type E != T
    return new E();
  }
}
class A extends B<C> {}

class C extends D<A> {}

class B<U> {
  function foo(){}
}

class D<V> {}

class Generic7 {
  function test(){
    var tmp = new A();
    tmp.fo<caret>o();
  }
}
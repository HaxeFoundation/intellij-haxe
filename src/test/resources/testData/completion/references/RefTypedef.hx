typedef Ref<T> = {
  function get():T;
}

class Main {
  function foo(a:Ref<String>) {
    a.get().<caret>
  }
}
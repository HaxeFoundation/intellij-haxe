package ;
class X<T> {
  public function new() {}
  public function create() : T {
    return new T();
  }
}

class Main {
  public function main() {
    var v:X<X<Int>>=new X<X<Int>>();
  }
}
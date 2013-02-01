class Macro {
  @:macro @:keep static function fib(n:Int) {
    if(n < 2) return 1;
    return fib(n-2) + fib(n-1);
  }
}

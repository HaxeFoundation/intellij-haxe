package;

abstract W (Int) from Int {
    @:to inline public function toString():String return '$this';
}

class Test {
  var arr2: Array<Int->Int> = [ a -> new W(1) ];
}
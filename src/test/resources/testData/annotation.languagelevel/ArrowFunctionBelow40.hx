class ArrowFunctionBelow40 {
  function f():Void {
    var g = function(x:Int) return x;
    var h = (x:Int) <error descr="Arrow functions require Haxe 4.0 or higher (module language level is 3.4)">-></error> x;
  }
}

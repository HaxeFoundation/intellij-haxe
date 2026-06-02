package;

#if macro
import haxe.macro.Expr;
#end

// Module-level macro functions. Calling them from normal code must treat every declared
// parameter as a real argument - module-level functions have no implicit `this`.
macro function build(value:String):ExprOf<String> {
  return macro Std.string($v{value});
}

macro function nbuild(a:String, b:String, num:ExprOf<Int>):ExprOf<String> {
  return macro Std.string($v{a});
}

class Test {
  public static function use(n:Int) {
    build("hello");
    var s:String = build("world");
    nbuild("foo", "bar", n);
    var t:String = nbuild("baz", "qux", n);
  }
}

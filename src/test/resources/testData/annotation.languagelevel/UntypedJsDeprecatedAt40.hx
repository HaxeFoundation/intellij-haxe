class UntypedJsDeprecatedAt40 {
  function f():Void {
    untyped <warning descr="__js__ is deprecated since Haxe 4.0; use js.Syntax.code">__js__</warning>("console.log(1)");
  }
}

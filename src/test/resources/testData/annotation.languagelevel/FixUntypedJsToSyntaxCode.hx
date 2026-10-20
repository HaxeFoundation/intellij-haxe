class FixUntypedJsToSyntaxCode {
  function f():Void {
    untyped <warning descr="__js__ is deprecated since Haxe 4.0; use js.Syntax.code">__j<caret>s__</warning>("console.log(1)");
  }
}

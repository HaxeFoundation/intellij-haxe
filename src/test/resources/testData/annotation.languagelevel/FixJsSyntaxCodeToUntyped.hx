class FixJsSyntaxCodeToUntyped {
  function f():Void {
    <warning descr="js.Syntax.code is not available before Haxe 4.0 (module language level is 3.4)">js.Syntax.c<caret>ode</warning>("console.log(1)");
  }
}

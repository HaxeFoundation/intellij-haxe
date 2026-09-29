class FixJsSyntaxCodeToUntyped {
  function f():Void {
    js.Syntax.c<caret>ode("console.log(1)");
  }
}

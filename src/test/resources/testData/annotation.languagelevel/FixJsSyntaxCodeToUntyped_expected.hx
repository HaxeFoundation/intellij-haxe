class FixJsSyntaxCodeToUntyped {
  function f():Void {
    untyped __js__("console.log(1)");
  }
}

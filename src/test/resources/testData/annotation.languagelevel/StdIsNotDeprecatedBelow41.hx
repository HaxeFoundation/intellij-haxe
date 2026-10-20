class StdIsNotDeprecatedBelow41 {
  function f(o:Dynamic):Bool {
    return Std.is(o, String);
  }
}

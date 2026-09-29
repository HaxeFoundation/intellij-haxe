class FixStdIsOfTypeToStdIs {
  function f(o:Dynamic):Bool {
    return Std.isOf<caret>Type(o, String);
  }
}

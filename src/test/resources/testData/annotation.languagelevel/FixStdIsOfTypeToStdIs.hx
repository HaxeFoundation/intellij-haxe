class FixStdIsOfTypeToStdIs {
  function f(o:Dynamic):Bool {
    return <warning descr="Std.isOfType is not available before Haxe 4.1 (module language level is 4.0)">Std.isOf<caret>Type</warning>(o, String);
  }
}

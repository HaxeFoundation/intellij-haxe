class StdIsDeprecatedAt41 {
  function f(o:Dynamic):Bool {
    return <warning descr="Std.is is deprecated since Haxe 4.1; use Std.isOfType">Std.is</warning>(o, String);
  }
}

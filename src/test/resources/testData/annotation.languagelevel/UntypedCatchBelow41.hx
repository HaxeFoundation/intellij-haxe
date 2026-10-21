class UntypedCatchBelow41 {
  function f():Void {
    try { } catch (<error descr="Untyped catch parameters require Haxe 4.1 or higher (module language level is 4.0)">e</error>) { }
  }
}

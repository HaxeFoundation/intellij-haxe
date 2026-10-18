class FixExternMetaToKeyword {
  <warning descr="@:extern is deprecated since Haxe 4.0; use extern">@:ext<caret>ern</warning> public inline function f():Int return 1;
}

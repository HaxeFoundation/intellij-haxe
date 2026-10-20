class FixExternModifierToMeta {
  <error descr="extern field modifiers require Haxe 4.0 or higher (module language level is 3.4)">ext<caret>ern</error> public inline function f():Int return 1;
}

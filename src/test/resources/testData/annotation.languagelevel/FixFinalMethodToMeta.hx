class FixFinalMethodToMeta {
  public <error descr="final modifiers require Haxe 4.0 or higher (module language level is 3.4)">fin<caret>al</error> function f():Void {}
}

class FinalKeywordBelow40 {
  function f():Void {
    <error descr="final modifiers require Haxe 4.0 or higher (module language level is 3.4)">final</error> x = 1;
  }
}

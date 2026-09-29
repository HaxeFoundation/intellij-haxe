class NullCoalescingBelow43 {
  function f():Void {
    var a:Null<Int> = null;
    var b = <error descr="Null coalescing operators require Haxe 4.3 or higher (module language level is 4.2)">a ?? 1</error>;
  }
}

class NullCoalescingAt43 {
  function f():Void {
    var a:Null<Int> = null;
    var b = a ?? 1;
  }
}

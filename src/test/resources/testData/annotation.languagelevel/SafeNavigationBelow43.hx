class SafeNavigationBelow43 {
  function f(s:String):Void {
    var l = s<error descr="Safe navigation operators require Haxe 4.3 or higher (module language level is 4.2)">?.</error>length;
  }
}

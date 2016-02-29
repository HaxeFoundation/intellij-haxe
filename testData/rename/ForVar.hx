class Bar {
  public function new() {}
  function doSomething():String return "";
}
class Foo {
  var a:Bar = new Bar();
  function printNtimes(msg:String, n:Int):Void for(a in 0...n) trace(a<caret> + ":" + msg);
}
class Bar {
  public function new() {}
  function doSomething():String return "";
}
class Foo {
  var a:Bar = new Bar();
  function printNtimes(msg:String, n:Int):Void for(index in 0...n) trace(index + ":" + msg);
}
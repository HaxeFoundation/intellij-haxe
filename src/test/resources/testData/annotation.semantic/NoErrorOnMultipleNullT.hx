package;
class Test{
  var x:Null<String> = null;
  var y:Null<Test> = null;
  function new() {
    x = "New String";
    y = this; // <-- Should not have error: "Incompatible types; Test should be Null<Test>."
  }
}

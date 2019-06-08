package;

@:forward abstract MyString(String) to String from String {
  public function doubleLength() { return this.length() * 2; }
  function halfLength() { return this.length() / 2; }  // private, shouldn't be included in completion
}

class Test {
  var ms : Null<MyString> = null;

  public function new() {
    ms = "SomeString";
    ms.<caret>doubleLength();
  }
}
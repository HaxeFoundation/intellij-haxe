class ParenthesizedArrayAccess {
    public function arrayAccess() {
        var testee = new MyClass();
        trace((testee.listItems)[0]);  // <<--- Errors were on [ and ]
    }
}

class MyClass {
  public function new() {}
  public var listItems : Array<Int> = [ 1, 2, 3, 4, 5 ];
}

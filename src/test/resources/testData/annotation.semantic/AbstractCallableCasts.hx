class AbstractCallableCasts {
  public function testCalls() {
    //Normal
    var fn:MyDirectCast;
    var ret:String = fn(1); // correct
    var ret:String = fn(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"1"</error>); // wrong (incorrect parameter type)

    var fn:MyImplicitCast;
    var ret:String = fn(1); // correct
    var ret:String = fn(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"1"</error>); // wrong (incorrect parameter type)

    //Generic

    var fn:MyGenericDirectCast<String>;
    var ret:String = fn("1"); // correct
    var ret:String = fn(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error> ); // wrong (incorrect parameter type)

    var fn:MyGenericImplicitCast<String>;
    var ret:String = fn("1"); // correct
    var ret:String = fn(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // wrong (incorrect parameter type)
  }
}

abstract MyDirectCast(Int->String) to  Int->String{}
abstract MyImplicitCast(Int->String){

  @:to
  function toFn() {
    return this;
  }
}
abstract MyGenericDirectCast<T>(T->String) to  T->String{}
abstract MyGenericImplicitCast<T>(T->String) {

  @:to
  function toFn() {
    return this;
  }
}
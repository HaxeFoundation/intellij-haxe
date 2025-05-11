class MyClass {
  public function classMethod() {}
}

abstract MyAbs(MyClass) {
  function abstractMethod() {}

  function test() {

    // CORRECT

    var x:MyClass = this;
    var x:MyAbs = abstract;

    abstractMethod(); // correct (default is abstract / current class)
    abstract.abstractMethod(); // correct  (abstract = "current class")
    this.classMethod(); // correct (this = underlying type)

    // WRONG

    <warning descr="Unresolved symbol">classMethod()</warning>; // wrong (abstract does not have method with this name)

    var x:MyAbs = <error descr="Incompatible type: MyClass should be MyAbs">this</error>; // wrong (type mismatch)
    var x:MyClass = <error descr="Incompatible type: MyAbs should be MyClass">abstract</error>; // wrong (type mismatch)

    this.<warning descr="Unresolved symbol">abstractMethod()</warning>; // wrong ( underlying type does not have this method)
    abstract.<warning descr="Unresolved symbol">classMethod()</warning>; // wrong  (abstract does not have this method)

      // super can not be used in abstracts
    super.<warning descr="Unresolved symbol">classMethod()</warning>;
    super.<warning descr="Unresolved symbol">abstractMethod()</warning>;
  }
}
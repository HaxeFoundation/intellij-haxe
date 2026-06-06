package;

class Data {
  public var flagA:Bool;
  public var flagB:Bool;
  public function new() { flagA = true; flagB = true; }
}

typedef Spec = {a:Bool, b:Bool};

class Base {
  private var data:Data;
  public function new() { data = new Data(); }
}

class Child extends Base {
  public function new() { super(); }

  public function makeSpec():Spec {
    return {
      a: data.flagA,
      b: data.flagB
    };
  }
  public function shouldFail():Spec {
    // verifying that unresolved and wrong type refs are still annotated
    return {
      a: data.<warning descr="Unresolved symbol">DoesNotExist</warning>, // Not found
      <error descr="have 'b:Data' wants 'b:Bool'">b: data</error> // wrong type
    };
  }
}

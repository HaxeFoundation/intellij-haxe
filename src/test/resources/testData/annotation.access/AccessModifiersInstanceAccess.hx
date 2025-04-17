package ;
class AccessModifiersInstanceAccess {
  public function testPublic() {
    var instance:PublicStaticMembers = null;
    instance.varable;
    instance.property;
    instance.method();
    instance.method;
  }

  public function testExternDefault() {
    var instance:DefaultExternMembers = null;
    instance.varable;
    instance.property;
    instance.method();
    instance.method;
  }

  public function testExternDefaultInterface() {
    var instance:ExternInterfaceMembers = null;
    instance.varable;
    instance.property;
    instance.method();
    instance.method;
  }

  // EXPECTED : Cannot access private field varable/property/method
  public function testPrivate() {
    var instance:PrivateStaticMembers = null;
    instance.<error descr="Cannot access private field varable">varable</error>;
    instance.<error descr="Cannot access private field property">property</error>;
    instance.<error descr="Cannot access private field method">method</error>();
    instance.<error descr="Cannot access private field method">method</error>;
  }

  // EXPECTED : Cannot access private field varable/property/method
  public function testDefault() {
    var instance:DefaultStaticMembers = null;
    instance.<error descr="Cannot access private field varable">varable</error>;
    instance.<error descr="Cannot access private field property">property</error>;
    instance.<error descr="Cannot access private field method">method</error>();
    instance.<error descr="Cannot access private field method">method</error>;
  }
}

class PublicStaticMembers  {
  public var varable:Int;
  public var property(default,default):Int;
  public function method():Void{}
}
class PrivateStaticMembers  {
  private var varable:Int;
  private var property(default,default):Int;
  private function method():Void{}
}

class DefaultStaticMembers  {
  var varable:Int;
  var property(default,default):Int;
  function method():Void{}
}

extern class DefaultExternMembers  {
  var varable:Int;
  var property(default,default):Int;
  function method():Void{}
}

extern interface ExternInterfaceMembers  {
  var varable:Int;
  var property(default,default):Int;
  function method():Void{}
}

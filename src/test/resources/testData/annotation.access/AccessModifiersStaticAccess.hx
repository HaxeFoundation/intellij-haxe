package ;

class AccessModifiersStaticAccess {
  public function testPublic() {
    PublicStaticMembers.varable;
    PublicStaticMembers.property;
    PublicStaticMembers.method();
    PublicStaticMembers.method;
  }

  public function testExternDefault() {
    DefaultExternMembers.varable;
    DefaultExternMembers.property;
    DefaultExternMembers.method();
    DefaultExternMembers.method;
  }

  public function testExternDefaultInterface() {
    ExternInterfaceMembers.varable;
    ExternInterfaceMembers.property;
    ExternInterfaceMembers.method();
    ExternInterfaceMembers.method;
  }

  // EXPECTED : Cannot access private field varable/property/method
  public function testPrivate() {
    PrivateStaticMembers.<error descr="Cannot access private field varable">varable</error>;
    PrivateStaticMembers.<error descr="Cannot access private field property">property</error>;
    PrivateStaticMembers.<error descr="Cannot access private field method">method</error>();
    PrivateStaticMembers.<error descr="Cannot access private field method">method</error>;
  }

  // EXPECTED : Cannot access private field varable/property/method
  public function testDefault() {
    DefaultStaticMembers.<error descr="Cannot access private field varable">varable</error>;
    DefaultStaticMembers.<error descr="Cannot access private field property">property</error>;
    DefaultStaticMembers.<error descr="Cannot access private field method">method</error>();
    DefaultStaticMembers.<error descr="Cannot access private field method">method</error>;
  }
}

class PublicStaticMembers  {
  public static var varable:Int;
  public static var property(default,default):Int;
  public static function method():Void{}
}
class PrivateStaticMembers  {
  private static var varable:Int;
  private static var property(default,default):Int;
  private static function method():Void{}
}

class DefaultStaticMembers  {
  static var varable:Int;
  static var property(default,default):Int;
  static function method():Void{}
}

extern class DefaultExternMembers  {
  static var varable:Int;
  static var property(default,default):Int;
  static function method():Void{}
}

extern interface ExternInterfaceMembers  {
  static var varable:Int;
  static var property(default,default):Int;
  static function method():Void{}
}


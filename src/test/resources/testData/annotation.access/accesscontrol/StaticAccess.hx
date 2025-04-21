package accesscontrol;

import accesscontrol.StaticAccess.TestData as Alias;

typedef TDef = TestData;

class StaticAccessTest {
    public function new() {
        var instance:TestData = null;
        instance.instanceVariable;// correct
        instance.instanceMethod();// correct

        instance.<error descr="Cannot access static field classVariable from a class instance">classVariable</error>; // wrong
        instance.<error descr="Cannot access static field classMethod from a class instance">classMethod</error>(); // wrong

        TestData.classVariable; // correct
        TestData.classMethod(); // correct

        TestData.<error descr="Static access to instance field instanceVariable is not allowed ">instanceVariable</error>;  // Wrong
        TestData.<error descr="Static access to instance field instanceMethod is not allowed ">instanceMethod</error>(); // Wrong

        TDef.classVariable; // correct
        TDef.classMethod(); // correct

        TDef.<error descr="Static access to instance field instanceVariable is not allowed ">instanceVariable</error>;  // Wrong
        TDef.<error descr="Static access to instance field instanceMethod is not allowed ">instanceMethod</error>(); // Wrong

        Alias.classVariable; // correct
        Alias.classMethod(); // correct

        Alias.<error descr="Static access to instance field instanceVariable is not allowed ">instanceVariable</error>;  // Wrong
        Alias.<error descr="Static access to instance field instanceMethod is not allowed ">instanceMethod</error>(); // Wrong

        var constrcutor = StaticAccessTest.new; // correct : static access to constructor should be allowed
    }
}

class TestData {

    public var instanceVariable:Int;

    public function instanceMethod() {}
    public static var classVariable:Int;
    public static function classMethod() {}
}

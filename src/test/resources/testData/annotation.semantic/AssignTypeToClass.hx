package ;
typedef DefClass = TestClass;
class ClassTypeAssignmentTest {
    public function new() {

        var myString = "test string";
        var myClassObject:ClassTypeAssignmentTest = new ClassTypeAssignmentTest();
        var myEnumObject:TestEnum = TestEnum.SOME_VALUE;

        //Class<Dynamic>
        var dynamicClass1:Class<Dynamic> = TestClass;        // [OK] type is Class
        var dynamicClass2:Class<Dynamic> = TestExtended;     // [OK] type is Class
        var dynamicClass3:Class<Dynamic> = null;             // [OK] Null allowed
        var dynamicClass4:Class<Dynamic> = String;           // [OK] allow since its Dynamic
        var dynamicClass5:Class<Dynamic> = OtherClass;       // [OK] allow since its Dynamic

        var <error descr="Incompatible type: String should be Class<Dynamic>">dynamicClass6:Class<Dynamic> = ""</error> ;                               // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<Dynamic>">dynamicClass7:Class<Dynamic> = myString</error>;                          // [Wrong] not a Class
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<Dynamic>">dynamicClass8:Class<Dynamic> = myClassObject</error>;    // [Wrong] not a Class
        var <error descr="Incompatible type: OtherEnum should be Class<Dynamic>">dynamicClass9:Class<Dynamic> = OtherEnum</error>;                      // [Wrong] incompatible types Enum Vs Class

        //Class<Any>
	var anyClass1:Class<Any> = null;                    // [OK] Null allowed

        var <error descr="Incompatible type: TestClass should be Class<Any>">anyClass2:Class<Any> = TestClass</error>;                    // [Wrong] type need to be casted
        var <error descr="Incompatible type: TestExtended should be Class<Any>">anyClass3:Class<Any> = TestExtended</error>;              // [Wrong] type need to be casted
        var <error descr="Incompatible type: String should be Class<Any>">anyClass4:Class<Any> = String</error>;                          // [Wrong] type need to be casted
        var <error descr="Incompatible type: OtherClass should be Class<Any>">anyClass5:Class<Any> = OtherClass</error>;                  // [Wrong] type need to be casted
        var <error descr="Incompatible type: String should be Class<Any>">anyClass6:Class<Any> = ""</error>;                              // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<Any>">anyClass7:Class<Any> = myString</error>;                        // [Wrong] not a Class
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<Any>">anyClass8:Class<Any> = myClassObject</error>;  // [Wrong] not a Class
        var <error descr="Incompatible type: OtherEnum should be Class<Any>">anyClass9:Class<Any> = OtherEnum</error>;                    // [Wrong] Enum not a Class

        //Class<Type>
        var specificClass01:Class<TestClass> = TestClass ;             // [OK] type is the same class
        var specificClass02:Class<TestClass> = DefClass ;              // [OK] type is the same class
        var specificClass03:Class<TestClass> = TestExtended ;          // [OK] type is extending the Class
        var specificClass04:Class<TestClass> = null;                   // [OK] Null allowed

        var <error descr="Incompatible type: String should be Class<TestClass>">specificClass05:Class<TestClass> = ""</error>;                                      // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<TestClass>">specificClass06:Class<TestClass> = myString</error>;                                // [Wrong] not a Class
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<TestClass>">specificClass07:Class<TestClass> = myClassObject</error>;          // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<TestClass>">specificClass08:Class<TestClass> = String</error>;                                  // [Wrong] Wrong Class
        var <error descr="Incompatible type: OtherClass should be Class<TestClass>">specificClass09:Class<TestClass> = OtherClass</error>;                          // [Wrong] Wrong Class
        var <error descr="Incompatible type: OtherEnum should be Class<TestClass>">specificClass10:Class<TestClass> = OtherEnum</error>;                            // [Wrong] incompatible types Enum Vs Class

        //Class<Interface>
        var interfaceClass1:Class<Inter> = TestExtended;              // [OK] type is  implementing
        var interfaceClass2:Class<Inter> = SecondExtended;            // [OK]  base is implementing

      var <error descr="Incompatible type: TestClass should be Class<Inter>">interfaceClass3:Class<Inter> = TestClass</error>;      // [wrong] type is not implementing interface




    }

}
interface Inter{}
class TestClass {}
class TestExtended extends TestClass implements Inter {}
class SecondExtended extends TestExtended{}
class OtherClass {}
enum TestEnum {
    SOME_VALUE;
}
enum OtherEnum {}

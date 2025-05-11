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

        var dynamicClass6:Class<Dynamic> = <error descr="Incompatible type: String should be Class<Dynamic>">""</error> ;                               // [Wrong] not a Class
        var dynamicClass7:Class<Dynamic> = <error descr="Incompatible type: String should be Class<Dynamic>">myString</error>;                          // [Wrong] not a Class
        var dynamicClass8:Class<Dynamic> = <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<Dynamic>">myClassObject</error>;    // [Wrong] not a Class
        var dynamicClass9:Class<Dynamic> = <error descr="Incompatible type: Enum<OtherEnum> should be Class<Dynamic>">OtherEnum</error>;                // [Wrong] incompatible types Enum Vs Class

        //Class<Any>
        var anyClass1:Class<Any> = null;                    // [OK] Null allowed

        // Any(Dynamic) got changed in 4.3 and "from Dynamic" was added allowing any class
        var anyClass2:Class<Any> = TestClass;      // [Correct for 4.3 and newer]
        var anyClass3:Class<Any> = TestExtended;   // [Correct for 4.3 and newer]
        var anyClass4:Class<Any> = String;         // [Correct for 4.3 and newer]
        var anyClass5:Class<Any> = OtherClass;     // [Correct for 4.3 and newer]

        var anyClass6:Class<Any> = <error descr="Incompatible type: String should be Class<Any>">""</error>;                                     // [Wrong] not a Class
        var anyClass7:Class<Any> = <error descr="Incompatible type: String should be Class<Any>">myString</error>;                               // [Wrong] not a Class
        var anyClass8:Class<Any> = <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<Any>">myClassObject</error>;         // [Wrong] not a Class
        var anyClass9:Class<Any> = <error descr="Incompatible type: Enum<OtherEnum> should be Class<Any>">OtherEnum</error>;                     // [Wrong] Enum not a Class

        //Class<Type>
        var specificClass01:Class<TestClass> = TestClass ;             // [OK] type is the same class
        var specificClass02:Class<TestClass> = DefClass ;              // [OK] type is the same class
        var specificClass03:Class<TestClass> = TestExtended ;          // [OK] type is extending the Class
        var specificClass04:Class<TestClass> = null;                   // [OK] Null allowed

        var specificClass05:Class<TestClass> = <error descr="Incompatible type: String should be Class<TestClass>">""</error>;                                      // [Wrong] not a Class
        var specificClass06:Class<TestClass> = <error descr="Incompatible type: String should be Class<TestClass>">myString</error>;                                // [Wrong] not a Class
        var specificClass07:Class<TestClass> = <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<TestClass>">myClassObject</error>;          // [Wrong] not a Class
        var specificClass08:Class<TestClass> = <error descr="Incompatible type: Class<String> should be Class<TestClass>">String</error>;                           // [Wrong] Wrong Class
        var specificClass09:Class<TestClass> = <error descr="Incompatible type: Class<OtherClass> should be Class<TestClass>">OtherClass</error>;                   // [Wrong] Wrong Class
        var specificClass10:Class<TestClass> = <error descr="Incompatible type: Enum<OtherEnum> should be Class<TestClass>">OtherEnum</error>;                      // [Wrong] incompatible types Enum Vs Class

        //Class<Interface>
        var interfaceClass1:Class<Inter> = TestExtended;              // [OK] type is  implementing
        var interfaceClass2:Class<Inter> = SecondExtended;            // [OK]  base is implementing

        var interfaceClass3:Class<Inter> = <error descr="Incompatible type: Class<TestClass> should be Class<Inter>">TestClass</error>;      // [wrong] type is not implementing interface




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

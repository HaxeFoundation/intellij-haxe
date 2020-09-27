package ;
typedef DefClass = TestClass;
class ClassTypeAssignmentTest {
    public function new() {

        var myString = "test string";
        var myClassObject:ClassTypeAssignmentTest = new ClassTypeAssignmentTest();
        var myEnumObject:TestEnum = TestEnum.SOME_VALUE;

        //Enum<Dynamic>
        var dynamicEnum01:Enum<Dynamic> = TestEnum;            // [OK] type is enum
        var dynamicEnum02:Enum<Dynamic> = null;                // [OK] Null allowed

        var <error descr="Incompatible type: String should be Enum<Dynamic>">dynamicEnum03:Enum<Dynamic> = ""</error>;                                  // [Wrong] Not enum Type
        var <error descr="Incompatible type: String should be Enum<Dynamic>">dynamicEnum04:Enum<Dynamic> = myString</error>;                            // [Wrong] Not enum Type
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Enum<Dynamic>">dynamicEnum05:Enum<Dynamic> = myClassObject</error>;      // [Wrong] Not enum Type
        var <error descr="Incompatible type: TestEnum should be Enum<Dynamic>">dynamicEnum06:Enum<Dynamic> = myEnumObject</error>;                      // [Wrong] Not enum Type (but enum value)
        var <error descr="Incompatible type: String should be Enum<Dynamic>">dynamicEnum07:Enum<Dynamic> = String</error> ;                             // [Wrong] incompatible types Enum Vs Class
        var <error descr="Incompatible type: TestEnum should be Enum<Dynamic>">dynamicEnum08:Enum<Dynamic> = TestEnum.SOME_VALUE</error>;               // [Wrong] incompatible types Enum Vs Class
        var <error descr="Incompatible type: OtherClass should be Enum<Dynamic>">dynamicEnum09:Enum<Dynamic> = OtherClass</error>;                       // [Wrong] incompatible types Enum Vs Class


        //Enum<Enum>
        var specificEnum01:Enum<TestEnum> = TestEnum;            // [OK] Correct Enum Type
        var specificEnum02:Enum<TestEnum> = null;                // [OK] Null allowed

        var <error descr="Incompatible type: String should be Enum<TestEnum>">specificEnum03:Enum<TestEnum> = ""</error>;                                 // [Wrong] Not enum Type
        var <error descr="Incompatible type: String should be Enum<TestEnum>">specificEnum04:Enum<TestEnum> = myString</error>;                           // [Wrong] Not enum Type
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Enum<TestEnum>">specificEnum05:Enum<TestEnum> = myClassObject</error>;     // [Wrong] Not enum Type
        var <error descr="Incompatible type: TestEnum should be Enum<TestEnum>">specificEnum06:Enum<TestEnum> = myEnumObject</error>;                     // [Wrong] Not enum Type (but enum value)
        var <error descr="Incompatible type: String should be Enum<TestEnum>">specificEnum07:Enum<TestEnum> = String</error> ;                            // [Wrong] incompatible types Enum Vs Class
        var <error descr="Incompatible type: TestEnum should be Enum<TestEnum>">specificEnum08:Enum<TestEnum> = TestEnum.SOME_VALUE</error>;              // [Wrong] incompatible types Enum Vs Class
        var <error descr="Incompatible type: OtherClass should be Enum<TestEnum>">specificEnum09:Enum<TestEnum> = OtherClass</error>;                     // [Wrong] incompatible types Enum Vs Class
        var <error descr="Incompatible type: OtherEnum should be Enum<TestEnum>">specificEnum10:Enum<TestEnum> = OtherEnum</error>;                       // [Wrong] wrong Enum


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

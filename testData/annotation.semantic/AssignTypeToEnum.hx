package ;
class ClassTypeAssignmentTest {
    public function new() {

        var myString = "test string";
        var myClassObject:ClassTypeAssignmentTest = new ClassTypeAssignmentTest();
        var myEnumObject:TestEnum = TestEnum.SOME_VALUE;

        // correct use
        var specificEnumTemplate1:Enum<TestEnum> = TestEnum;            // [OK] Correct Enum Type
        var specificEnumTemplate2:Enum<TestEnum> = null;                // [OK] Null allowed

        // incorrect use
        var <error descr="Incompatible type: String should be Enum<TestEnum>">specificEnumTemplate3:Enum<TestEnum> = ""</error>;                                    // [Wrong] Not enum Type
        var <error descr="Incompatible type: String should be Enum<TestEnum>">specificEnumTemplate4:Enum<TestEnum> = myString</error>;                              // [Wrong] Not enum Type
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Enum<TestEnum>">specificEnumTemplate5:Enum<TestEnum> = myClassObject</error>;        // [Wrong] Not enum Type
        var specificEnumTemplate5:Enum<TestEnum> = myEnumObject;        // [Wrong] Not enum Type (but enum value)
        var <error descr="Incompatible type: String should be Enum<TestEnum>">specificEnumTemplate6:Enum<TestEnum> = String</error> ;                               // [Wrong] incompatible types Enum Vs Class
        var <error descr="Incompatible type: OtherClass should be Enum<TestEnum>">specificEnumTemplate7:Enum<TestEnum> = OtherClass</error>;                        // [Wrong] incompatible types Enum Vs Class
        var <error descr="Incompatible type: OtherEnum should be Enum<TestEnum>">specificEnumTemplate7:Enum<TestEnum> = OtherEnum</error>;                          // [Wrong] wrong Enum

    }
}
// helper classes
class TestClass {}
class TestExtended extends TestClass {}
class OtherClass {}
enum TestEnum {SOME_VALUE;}
enum OtherEnum {}
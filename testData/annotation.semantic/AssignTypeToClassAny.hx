package ;
class ClassTypeAssignmentTest {
    public function new() {

        var myString = "test string";
        var myClassObject:ClassTypeAssignmentTest = new ClassTypeAssignmentTest();
        var myEnumObject:TestEnum = TestEnum.SOME_VALUE;

        // correct use
        var anyTemplateOk1:Class<Any> = TestClass;               // [OK] type is Class
        var anyTemplateOk2:Class<Any> = TestExtended;            // [OK] type is Class
        var anyTemplateOk3:Class<Any> = null;                    // [OK] Null allowed
        var anyTemplateOk4:Class<Any> = String;                  // [OK] allow since its Dynamic
        var anyTemplateOk5:Class<Any> = OtherClass;              // [OK] allow since its Dynamic

        // incorrect use
        var <error descr="Incompatible type: String should be Class<Any>">anyTemplateWrong1:Class<Any> = ""</error>;                                       // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<Any>">anyTemplateWrong2:Class<Any> = myString</error>;                                 // [Wrong] not a Class
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<Any>">anyTemplateWrong3:Class<Any> = myClassObject</error>;           // [Wrong] not a Class
        var <error descr="Incompatible type: OtherEnum should be Class<Any>">anyTemplateWrong4:Class<Any> = OtherEnum</error>;                             // [Wrong] incompatible types Enum Vs Class

    }
}
// helper classes
class TestClass {}
class TestExtended extends TestClass {}
class OtherClass {}
enum TestEnum {SOME_VALUE;}
enum OtherEnum {}
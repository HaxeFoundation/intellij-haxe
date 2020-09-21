package ;
class ClassTypeAssignmentTest {
    public function new() {

        var myString = "test string";
        var myClassObject:ClassTypeAssignmentTest = new ClassTypeAssignmentTest();
        var myEnumObject:TestEnum = TestEnum.SOME_VALUE;

        // correct use
        var specificTemplateOk1:Class<TestClass> = TestClass ;             // [OK] type is the same class
        var specificTemplateOk2:Class<TestClass> = TestExtended;           // [OK] type is extending the Class
        var specificTemplateOk3:Class<TestClass> = null;                   // [OK] Null allowed

        // incorrect use
        var <error descr="Incompatible type: String should be Class<TestClass>">specificTemplateWrong1:Class<TestClass> = ""</error>;                                     // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<TestClass>">specificTemplateWrong2:Class<TestClass> = myString</error>;                               // [Wrong] not a Class
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<TestClass>">specificTemplateWrong3:Class<TestClass> = myClassObject</error>;         // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<TestClass>">specificTemplateWrong4:Class<TestClass> = String</error>;                                 // [Wrong] Wrong Class
        var <error descr="Incompatible type: OtherClass should be Class<TestClass>">specificTemplateWrong5:Class<TestClass> = OtherClass</error>;                         // [Wrong] Wrong Class
        var <error descr="Incompatible type: OtherEnum should be Class<TestClass>">specificTemplateWrong6:Class<TestClass> = OtherEnum</error>;                           // [Wrong] incompatible types Enum Vs Class

    }
}
// helper classes
class TestClass {}
class TestExtended extends TestClass {}
class OtherClass {}
enum TestEnum {SOME_VALUE;}
enum OtherEnum {}
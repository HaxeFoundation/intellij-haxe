package ;
class ClassTypeAssignmentTest {
    public function new() {

        var myString = "test string";
        var myClassObject:ClassTypeAssignmentTest = new ClassTypeAssignmentTest();
        var myEnumObject:TestEnum = TestEnum.SOME_VALUE;

        // correct use
        var dynamicTemplateOk1:Class<Dynamic> = TestClass;        // [OK] type is Class
        var dynamicTemplateOk2:Class<Dynamic> = TestExtended;     // [OK] type is Class
        var dynamicTemplateOk3:Class<Dynamic> = null;             // [OK] Null allowed
        var dynamicTemplateOk4:Class<Dynamic> = String;           // [OK] allow since its Dynamic
        var dynamicTemplateOk5:Class<Dynamic> = OtherClass;       // [OK] allow since its Dynamic

        // incorrect use
        var <error descr="Incompatible type: String should be Class<Dynamic>">dynamicTemplateWrong1:Class<Dynamic> = ""</error>;                                // [Wrong] not a Class
        var <error descr="Incompatible type: String should be Class<Dynamic>">dynamicTemplateWrong2:Class<Dynamic> = myString</error>;                          // [Wrong] not a Class
        var <error descr="Incompatible type: ClassTypeAssignmentTest should be Class<Dynamic>">dynamicTemplateWrong3:Class<Dynamic> = myClassObject</error>;    // [Wrong] not a Class
        var <error descr="Incompatible type: OtherEnum should be Class<Dynamic>">dynamicTemplateWrong4:Class<Dynamic> = OtherEnum</error>;                      // [Wrong] incompatible types Enum Vs Class

    }
}
// helper classes
class TestClass {}
class TestExtended extends TestClass {}
class OtherClass {}
enum TestEnum {SOME_VALUE;}
enum OtherEnum {}
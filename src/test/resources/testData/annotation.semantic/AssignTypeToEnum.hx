package ;
typedef DefClass = TestClass;
class ClassTypeAssignmentTest {
    public function new() {

        var myString = "test string";
        var myClassObject:ClassTypeAssignmentTest = new ClassTypeAssignmentTest();
        var myEnumObject:TestEnum = TestEnum.SOME_VALUE;

        // enums with constructor if referenced is a function (needs a call expression for type)
        var myEnumFuctionRef:Int->TestEnum = TestEnum.SOME_CONSRUCTOR;
        var myEnumFuctionCall:TestEnum = TestEnum.SOME_CONSRUCTOR(1);

        //Enum<Dynamic>
        var dynamicEnum01:Enum<Dynamic> = TestEnum;            // [OK] type is enum
        var dynamicEnum02:Enum<Dynamic> = null;                // [OK] Null allowed

        var dynamicEnum03:Enum<Dynamic> = <error descr="Incompatible type: String should be Enum<Dynamic>">""</error>;                                  // [Wrong] Not enum Type
        var dynamicEnum04:Enum<Dynamic> = <error descr="Incompatible type: String should be Enum<Dynamic>">myString</error>;                            // [Wrong] Not enum Type
        var dynamicEnum05:Enum<Dynamic> = <error descr="Incompatible type: ClassTypeAssignmentTest should be Enum<Dynamic>">myClassObject</error>;      // [Wrong] Not enum Type
        var dynamicEnum06:Enum<Dynamic> = <error descr="Incompatible type: TestEnum should be Enum<Dynamic>">myEnumObject</error>;                      // [Wrong] Not enum Type (but enum value)
        var dynamicEnum07:Enum<Dynamic> = <error descr="Incompatible type: Class<String> should be Enum<Dynamic>">String</error> ;                      // [Wrong] incompatible types Enum Vs Class
        var dynamicEnum08:Enum<Dynamic> = <error descr="Incompatible type: SOME_VALUE should be Enum<Dynamic>">TestEnum.SOME_VALUE</error>;               // [Wrong] incompatible types Enum Vs Class
        var dynamicEnum09:Enum<Dynamic> = <error descr="Incompatible type: Class<OtherClass> should be Enum<Dynamic>">OtherClass</error>;               // [Wrong] incompatible types Enum Vs Class


        //Enum<Enum>
        var specificEnum01:Enum<TestEnum> = TestEnum;            // [OK] Correct Enum Type
        var specificEnum02:Enum<TestEnum> = null;                // [OK] Null allowed

        var specificEnum03:Enum<TestEnum> = <error descr="Incompatible type: String should be Enum<TestEnum>">""</error>;                                 // [Wrong] Not enum Type
        var specificEnum04:Enum<TestEnum> = <error descr="Incompatible type: String should be Enum<TestEnum>">myString</error>;                           // [Wrong] Not enum Type
        var specificEnum05:Enum<TestEnum> = <error descr="Incompatible type: ClassTypeAssignmentTest should be Enum<TestEnum>">myClassObject</error>;     // [Wrong] Not enum Type
        var specificEnum06:Enum<TestEnum> = <error descr="Incompatible type: TestEnum should be Enum<TestEnum>">myEnumObject</error>;                     // [Wrong] Not enum Type (but enum value)
        var specificEnum07:Enum<TestEnum> = <error descr="Incompatible type: Class<String> should be Enum<TestEnum>">String</error> ;                     // [Wrong] incompatible types Enum Vs Class
        var specificEnum08:Enum<TestEnum> = <error descr="Incompatible type: SOME_VALUE should be Enum<TestEnum>">TestEnum.SOME_VALUE</error>;              // [Wrong] incompatible types Enum Vs Class
        var specificEnum09:Enum<TestEnum> = <error descr="Incompatible type: Class<OtherClass> should be Enum<TestEnum>">OtherClass</error>;              // [Wrong] incompatible types Enum Vs Class
        var specificEnum10:Enum<TestEnum> = <error descr="Incompatible type: Enum<OtherEnum> should be Enum<TestEnum>">OtherEnum</error>;                 // [Wrong] wrong Enum


    }

}
interface Inter{}
class TestClass {}
class TestExtended extends TestClass implements Inter {}
class SecondExtended extends TestExtended{}
class OtherClass {}
enum TestEnum {
    SOME_VALUE;
    SOME_CONSRUCTOR(i:Int);
}
enum OtherEnum {}

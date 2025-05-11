package;
// EnumValueTools and EnumTools are included by the compiler and should resolve without import

enum TestEum {
    ValueA;
    ValueB;
}

// Methods from EnumValueTools/EnumTools should be usable as exntension methods (and should bind type)
class EnumToolsTest {
    public function correct() {
        var nameA:String = TestEum.ValueA.getName();
        var nameB:String = ValueB.getName();

        var constructors:Array<String> = TestEum.getConstructors();
        var enumName:String = TestEum.getName();

        var fromName:TestEum = TestEum.createByName(nameB);

        var fn1:Void -> Array<String> = TestEum.getConstructors;
        var fn2:(String, ?Array<Dynamic>) -> TestEum = TestEum.createByName;
        var fn3:Void -> String = ValueB.getName ;
    }

    public function wrong() {
        var nameA:Int = <error descr="Incompatible type: String should be Int">TestEum.ValueA.getName()</error>;
        var nameB:Int = <error descr="Incompatible type: String should be Int">ValueB.getName()</error>;

        var constructors:Array<Int> = <error descr="Incompatible type: Array<String> should be Array<Int>">TestEum.getConstructors()</error>;
        var enumName:Int = <error descr="Incompatible type: String should be Int">TestEum.getName()</error>;

        var fromName:Int = <error descr="Incompatible type: TestEum should be Int">TestEum.createByName("nameB")</error>;


        var name:Enum<Int> -> Array<Int> = <error descr="Incompatible type: Void->Array<String> should be Enum<Int>->Array<Int>">TestEum.getConstructors</error>;
        var name:(String, ?Array<Dynamic>) -> Int = <error descr="Incompatible type: (String, ?Array<Dynamic>)->TestEum should be (String, ?Array<Dynamic>)->Int">TestEum.createByName</error>;
        var name:Void -> Int = <error descr="Incompatible type: Void->String should be Void->Int">ValueB.getName</error>;
    }
}

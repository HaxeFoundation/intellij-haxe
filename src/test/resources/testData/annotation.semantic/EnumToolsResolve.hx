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
        var <error descr="Incompatible type: String should be Int">nameA:Int = TestEum.ValueA.getName()</error>;
        var <error descr="Incompatible type: String should be Int">nameB:Int = ValueB.getName()</error>;

        var <error descr="Incompatible type: Array<String> should be Array<Int>">constructors:Array<Int> = TestEum.getConstructors()</error>;
        var <error descr="Incompatible type: String should be Int">enumName:Int = TestEum.getName()</error>;

        var <error descr="Incompatible type: TestEum should be Int">fromName:Int = TestEum.createByName("nameB")</error>;


        var <error descr="Incompatible type: Void->Array<String> should be Enum<Int>->Array<Int>">name:Enum<Int> -> Array<Int> = TestEum.getConstructors</error>;
        var <error descr="Incompatible type: (String, ?Array<Dynamic>)->TestEum should be (String, ?Array<Dynamic>)->Int">name:(String, ?Array<Dynamic>) -> Int = TestEum.createByName</error>;
        var <error descr="Incompatible type: Void->String should be Void->Int">name:Void -> Int = ValueB.getName</error>;
    }
}

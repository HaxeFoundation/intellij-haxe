class TestStaticMethods {
    public function callExpressions() {
        // correct
        StaticMethods.test("Hello");
        StaticMethods.test(42);
        StaticMethods.test(42, "Hello");
        // wrong
        StaticMethods.test(<error descr="Too many arguments (expected 1 but got 2)\"">"42", "Hello"</error>);

    }
    public function functionTypes() {
        // correct
        var x:(String) -> String = StaticMethods.test;
        var x:(Int) -> Int = StaticMethods.test;
        var x:(Int, String) -> Map<Int, String> = StaticMethods.test;
        // wrong
        var x:(String, String) -> String = <error descr="Incompatible type: String->String should be (String, String)->String">StaticMethods.test</error>;


    }
    public function parameters() {
        // correct
        testParameters(StaticMethods.test("Hello"), StaticMethods.test(42), StaticMethods.test(42, "Hello"));
        testFunctionTypeParams(StaticMethods.test, StaticMethods.test, StaticMethods.test);
    }
}

class StaticMethods {
    public overload extern inline static function test(a:String):String {
        return a;
    }

    public overload extern inline static function test(i:Int):Int {
        return i;
    }

    public overload extern inline static function test(a:Int, b:String):Map<Int, String> {
        return [a => b];
    }
}

function testParameters(a:String, b:Int, c:Map<Int, String>) {}
function testFunctionTypeParams(a:(String) -> String, b:(Int) -> Int, c:(Int, String) -> Map<Int, String>) {}
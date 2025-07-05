class TestInstance {
    public function callExpressions() {
        var instance = new InstanceMethods();
//      correct
        instance.test("Hello");
        instance.test(42);
        instance.test(42, "Hello");

//        wrong
        instance.test(<error descr="Too many arguments (expected 1 but got 3)\"">42, "Hello", "lool"</error>);

    }
    public function functionTypes() {
        var instance = new InstanceMethods();
        // correct
        var x:(String) -> String = instance.test;
        var x:(Int) -> Int = instance.test;
        var x:(Int, String) -> Map<Int, String> = instance.test;

        // wrong
        var x:(String, String) -> String = <error descr="Incompatible type: String->String should be (String, String)->String">instance.test</error>;

    }
    public function parameters() {
        var instance = new InstanceMethods();

        // correct
        testParameters(instance.test("Hello"), instance.test(42), instance.test(42, "Hello"));
        testFunctionTypeParams(instance.test, instance.test, instance.test);
    }
}

class InstanceMethods {
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
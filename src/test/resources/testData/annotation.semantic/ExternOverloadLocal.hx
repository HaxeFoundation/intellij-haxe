class TestLocalMethods {
    public function callExpressions() {
        // correct
        var ok:String = test("Hello");
        var ok:Int = test(42);
        var ok:Map<Int, String> = test(42, "Hello");

        // wrong
        var wrongArguments = test(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"42"</error>, "Hello");
    }

    public function functionTypes() {
        // correct
        var x:(String) -> String = test;
        var x:(Int) -> Int = test;
        var x:(Int, String) -> Map<Int, String> = test;

        // wrong
        var x:(String, String) -> Void = <error descr="Incompatible type: String->String should be (String, String)->Void">test</error>;
    }

    public function parameters() {
        // correct
        testParameters(test("Hello"), test(42), test(42, "Hello"));
        //TODO
        testFunctionTypeParams(test, <error descr="Type mismatch (Expected: 'Int->Int' got: 'String->String')">test</error>, <error descr="Type mismatch (Expected: '(Int, String)->Map<Int, String>' got: 'String->String')">test</error>);

    }

    // overloads
    public extern inline overload function test(a:String):String {return a;}
    public extern inline overload function test(i:Int):Int {return i;}
    public extern inline overload function test(a:Int, b:String):Map<Int, String> {return [a => b];}

}

function testParameters(a:String, b:Int, c:Map<Int, String>) {}
function testFunctionTypeParams(a:(String) -> String, b:(Int) -> Int, c:(Int, String) -> Map<Int, String>) {}
package ;
class Test {
    function doSomething(k : String, v : String) {
        trace("I like " + k);
        trace("I mean " + Std.string(3.1416));
    }
    static function main() {
        var test = new Test();
        test.doSomething("Blueberry", <selection>Std.string(3.1416)</selection>);
    }
}
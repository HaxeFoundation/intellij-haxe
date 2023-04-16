package ;
class Test {
    var pi = Std.string(3.1416);

    function doSomething(k : String, v : String) {
        trace("I like " + k);
        trace("I mean " + pi);
    }
    static function main() {
        var test = new Test();
        test.doSomething("Blueberry", pi);
    }
}
abstract class BaseClass {
    public function new() {
        test();
    }

    abstract function test():Void;
}

class TestClassOne extends BaseClass {
    function test() {
        trace("one");
    }
}

class TestClassTwo extends BaseClass {
    function test() {
        trace("two");
    }
}

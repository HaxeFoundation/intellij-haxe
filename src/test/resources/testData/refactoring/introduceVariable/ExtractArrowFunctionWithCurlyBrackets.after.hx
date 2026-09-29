package;
class Test {
    function new() {
        var func = () -> {return this;};
        var getThis = func;
    }
}
class Void {
    function foo():Void {
        return inline bar();
    }

    function bar():String {
        return "";
    }
}
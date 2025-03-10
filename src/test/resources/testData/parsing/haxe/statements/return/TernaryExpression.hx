package ;
class TestTernaryExpression {
    public function Test1() {
        var b:Bool = true;
        return b ? "true" : "false";
    }

    public function Test2() {
        var s:String;
        var b:Bool = true;
        // important should be parsed as Ternary ( ref ? assign : assign)
        // if the expression priority is incorrect this might end up as assign expression as the root.
        b ? s = "true" : s = "false";
    }
    public function Test3() {
        var k:Bool = true;
        // just a sanity check to make sure also we can also parse AssignExpression with Ternary as child
        k = true ? false : true;
    }

    public function Test4() {
        var s:String;
        var b:Bool = true;
        return b ? s ?? "true" : s ?? "false";
    }

}
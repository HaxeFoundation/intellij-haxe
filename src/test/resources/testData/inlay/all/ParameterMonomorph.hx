class ParameterMonomorph {

    public function new() {
        var testFn/*<# :|String #>*/  = testFunction(null);
        var testCls/*<# :|TestClass|<|Int|> #>*/ = new TestClass(1);
    }
    function testFunction(?p/*<# :|String #>*/)/*<# :|String #>*/  {
        if (p == null) {
            p = "string";
        }
        return p;
    }
}
class TestClass<T> {
    var x:T;
    public function new(p1/*<# :|T #>*/) {
        x = p1;
    }
}
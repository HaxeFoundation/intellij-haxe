package ;
class TypeFromUsageHints<T, Q> {
    public var fromVarUsage  = new Map();

    public function new(x:T) {
        myVar = "";
        var y:Q;
        fromVarUsage.set(y, x);
    }

    public function getVar(){return fromVarUsage;}

    public function testFunction(xx, yy, zz) {
        var x:String = xx;
        var y:T = yy;
        var y:Q = zz;
        return yy;
    }
}

class TestClass {
    public function new() {
        var x/*<# :|TypeFromUsageHints|<|String|, |Int|> #>*/ =  new TypeFromUsageHints<String,Int>(null);

        // find typeParameters of member from usage
        var usageFromMember/*<# :|Map|<|Int|, |String|> #>*/ = x.fromVarUsage;
        var usageFromCall/*<# :|Map|<|Int|, |String|> #>*/ = x.getVar();
        var usageFunction/*<# :|(|)|->|Map|<|Int|, |String|> #>*/ = x.getVar;

        // finds parameter type from usage
        var testFn/*<# :|(|String|, |String|, |Int|)|->|String #>*/ = x.testFunction;
        var testRet/*<# :|String #>*/ = x.testFunction("1","2",3);
    }
}
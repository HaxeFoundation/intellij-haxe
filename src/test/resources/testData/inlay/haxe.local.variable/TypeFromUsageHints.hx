package ;
class TypeFromUsageHints<T, Q> {
    public var fromVarUsage  = new Map();
    public function getVar(){return fromVarUsage;}

    public function new(x:T) {
        myVar = "";
        var y:Q;
        fromVarUsage.set(y, x);
    }
}

class TestClass {
    public function new() {
        var x/*<# :TypeFromUsageHints<String, In… #>*/ =  new TypeFromUsageHints<String,Int>(null);
        var usageFromMember/*<# :Map<Int, String> #>*/ = x.fromVarUsage;
        var usageFromCall/*<# :Map<Int, String> #>*/ = x.getVar();
        var usageFunction/*<# :Void->Map<Int, String> #>*/ = x.getVar;
    }
}
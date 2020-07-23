package ;

class TestIssue943_DifferentiateVarAndFunctionTypes {
    public function new() {  }
    public function onAccountEnter(account:String, password:String):Void { }

    static public function main() {
        var o:OtherClass = new OtherClass();
        o.onAccountEnter = onAccountEnter;  // <-- Incorrect Error: Imcompatible type: Void should be Function
    }

    static public function voidStringAssignment() {
        var callback:Void->String;
        callback = function():String { return ""<error descr="Missing semicolon."> </error>};
        callback = function() { return ""<error descr="Missing semicolon."> </error>};  // With an inferred type.
        callback = ()->"";  // Arrow function.
    }
}

class OtherClass {
    public var onAccountEnter:String->String->Void = null;
    public function new() {}
}

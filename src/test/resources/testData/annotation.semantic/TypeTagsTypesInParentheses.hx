package;

class TypeTagsTypesInParentheses {
    // various test to confirm that types can be wrapped in Parenthesis

    public function testSingleWrapped(val:(Int)):(String) {
        var myVal:(Int) = val;
        return "";
    }
    public function testSingleWrappedMix(val:({x:Int})):(((String)->String)->String) {
        var myVal:({x:Int}) = val;
        return null;
    }

    public function testDoubleWrapped(val:((Int))):((String)) {
        var myVal:((Int)) = val;
        return "";
    }

    public function testDoubleWrappedFunction(val:((Int->String))):((Int->String)) {
        var myVal:((Int->String)) = val;
        return myVal;
    }

    public function testDoubleWrappedFunction2(val:(((Int)->String))):(((Int)->String)) {
        var myVal:(((Int)->String)) = val;
        return myVal;
    }
}

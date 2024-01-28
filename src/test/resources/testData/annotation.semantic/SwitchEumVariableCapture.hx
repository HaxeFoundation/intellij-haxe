package ;

enum Test<T> {
    TDoubleVal(s:String, i:Int);
    TString(s:String);
    TInt(i:Int);
    TAny(x:T);
    TNone;
}


class PatternMachingTest {
    public function testEumVariableCapture() {
        var myArray = ["String"];
        var  enumVal = Test.TAny(myArray);
        // correct
        switch(enumVal) {
            case TString(s): s.toLowerCase();
            case TInt(i): i  * 2;
            case TAny(a): a.indexOf("");
            case TDoubleVal(a, b): a.charAt(b) ;
            case TNone: null;
            case var value: trace(value);
        }
        //wrong
        switch(enumVal) {
            case TString(s): <error descr="Unable to apply operator * for types String and Int = 2">s * 2</error>; // WRONG
            case TInt(i): i.<warning descr="Unresolved symbol">length</warning>; // WRONG
            case TAny(a): a.indexOf(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>);
            case TDoubleVal(a, b): b.<warning descr="Unresolved symbol">charAt</warning>(a) ; // WRONG
        }
    }
}
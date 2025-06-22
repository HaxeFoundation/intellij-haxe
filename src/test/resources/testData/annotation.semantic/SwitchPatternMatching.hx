package ;

enum Test<T> {
    TDoubleVal(s:String, i:Int);
    TString(s:String);
    TInt(i:Int);
    TObject(o:{i:Int, s:String});
    TArray(x:Array<String>);
    TAny(x:T);
    TTest(t:Test<Dynamic>);
    TNone;
}


class PatternMachingTest {
    public function testEumVariableCapture() {
        var myArray = ["String"];
        var  enumVal = Test.TAny(myArray);

        // correct
        switch(enumVal) {
            case TString(x = "s"): x.toLowerCase();
            case TString(_ => _.length => 1):  trace(" 2x '=>' - OK");
            case TString(_ => _.toLowerCase() => "s"):  trace(" 2x '=>' - OK");
            case TNone | TString(_) : trace(" none OR string");
            case TTest(TNone | TString(_)): trace(" sub  OR");
            case TObject( x = {i:1}): x.s.toLowerCase();
            case TString(s): s.toLowerCase();
            case TInt(i): i  * 2;
            case TAny(a): a.indexOf("");
            case TDoubleVal(a, b): a.charAt(b) ;
            case TNone: null;
            case TArray(_.pop() => f ) : f.toLowerCase();
            case TAny(_.pop() => f) : f.toLowerCase();
            case TAny(var x) : trace(x);
            case var value: trace(value);
        }

        var nestedVal = TTest(enumVal);
        switch (nestedVal) {
            case TTest(TString(z)): z.toLowerCase();
            case TTest(TString(z.toLowerCase() => f  )): f.toLowerCase();
        }

        //wrong
        switch(enumVal) {
            case TString(s): <error descr="Unable to apply operator * for types String and Int = 2">s * 2</error>; // WRONG
            case TNone | TString(_): _.toLowerCase(); // TODO, while resolvable this one is not usable as it could be a match on TNone
            case TInt(i): i.<warning descr="Unresolved symbol">length</warning>; // WRONG
            case TAny(a): a.indexOf(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>);
            case TDoubleVal(a, b): b.<warning descr="Unresolved symbol">charAt</warning>(a) ; // WRONG
        }
    }

    public function testEnumExtractArray() {
        var myArray = ["String","string"];
        var enumVal = Test.TAny(myArray);

        switch([myArray, enumVal]) {
            case [_ => _.length => 2, TAny(extract  )]: extract.length;
            case [_, TAny(extract)]: extract.length;
            default : trace("default");
        }

        var myEnumArray = [enumVal];

        switch (myEnumArray) {
            case [ TAny(extract )]: extract.contains(""); // correct
            case [ TAny([s1, s2,_] )]: s1.toLowerCase() + s2.charAt(1); // Correct :extract elements from array
            case [ TAny(extract )]: extract.contains(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // wrong
        }

    }
    public function testEnumExtractObject() {
        var myArray = ["String"];
        var enumVal = Test.TAny(myArray);
        var someObj = {i:1, s:"str", e:enumVal};

        switch(someObj) {
            case {i:_, s:_, e:TAny(extract)}: extract.length; // correct
            case {i:_, s:_, e:TAny([arrayElement])}: arrayElement.toLowerCase();// correct (element from array)
            case {i:_, s:_, e:TAny(extract)}: <error descr="Unable to apply operator + for types Array<String> and Int = 1">extract + 1</error>;// wrong
            default : trace("default");
        }
    }
    public function testEnumExtractMixed() {
        var myArray = ["String"];
        var enumVal = Test.TAny(myArray);
        var someObj = {i:1, s:"str", e:enumVal};

        switch(["string", someObj, myArray]) {
            case [a, {i:_, s:_, e:TAny(extract)}, b]: {
                // testing all values
                extract.length + a.charAt(1) + b.pop().toLowerCase();
            }
            default : trace("default");
        }
    }


}
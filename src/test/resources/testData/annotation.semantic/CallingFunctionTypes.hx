package ;

typedef TypedefVoidFn = Void -> Void;

class Test {
    function new(){

        var voidFn:Void -> Void = function (){};
        voidFn(); // correct: void "argument" in signature is ignored
        voidFn(<error descr="Too many arguments (expected 0 but got 1)\"">1</error>); // Wrong: (no argument expected)

        var typeDefVoid:TypedefVoidFn = voidFn;
        typeDefVoid();

        var withTypeTag:Int -> String = testA;
        var withoutTypeTag = testA;
        var fromFunction = function (i:Int) return "";

        withTypeTag(<error descr="Type mismatch (Expected: 'Int' got: 'String')">""</error>);// wrong input
        withoutTypeTag(<error descr="Type mismatch (Expected: 'Int' got: 'String')">""</error>);// wrong input
        fromFunction(<error descr="Type mismatch (Expected: 'Int' got: 'String')">""</error>); // wrong input

        var assignA = withTypeTag(1);
        var assignB = withoutTypeTag(1);
        var assignC = fromFunction(1);

        assignA.toLowerCase(); // correct: assignA should resolve to string
        assignB.toLowerCase(); // correct: assignB should resolve to string
        assignC.toLowerCase(); // correct: assignC should resolve to string

        withTypeTag(1).toLowerCase();       // correct: return type should be string
        withoutTypeTag(1).toLowerCase();    // correct: return type should be string
        fromFunction(1).toLowerCase();      // correct: return type should be string

        var genericsFromTypeTag:String->String = testB; // correct : generics are set based on type in typetag

        var withoutTypeTagGeneric = testB;
        var genericResult = withoutTypeTagGeneric("");

        genericResult.toLowerCase(); // correct
        withoutTypeTagGeneric("").toLowerCase(); // correct

        // type tag from method generics not possible, should fail
        var wrongTypeTag:<warning descr="Unresolved symbol">T</warning>-><warning descr="Unresolved symbol">T</warning> = <error descr="Incompatible type: T->T should be T->T">testB</error>; // Wrong

    }

    public function  testA(i:Int):String {
        return "";
    }

    public function  testB<T>(i:T):T {
        return i;
    }
}
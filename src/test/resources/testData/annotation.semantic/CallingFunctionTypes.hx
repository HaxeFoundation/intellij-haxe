package ;

class Test {
    function new(){

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


        var <error descr="Incompatible type: T->T should be String->String">wrongTypeTagA:String->String = testB</error>; // Wrong : generic Types
        var wrongTypeTagB:<warning descr="Unresolved symbol">T</warning>-><warning descr="Unresolved symbol">T</warning> = testB; // Wrong : generic Types not available?

        var withoutTypeTagGeneric = testB;
        var genericResult = withoutTypeTagGeneric("");
        //TODO try to support generics
//        genericResult.toLowerCase(); // correct
//        withoutTypeTagGeneric("").toLowerCase(); // correct

    }

    public function  testA(i:Int):String {
        return "";
    }

    public function  testB<T>(i:T):T {
        return i;
    }
}
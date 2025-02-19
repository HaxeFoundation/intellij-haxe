class ArrayComprehensionsWithExpression {
    function testNormal() {

        // NORMAL COMPREHENSIONS

        //CORRECT
        var withIf:Array<Int>  = [for  (i in 0...6) if (i % 2 == 0) i];
        var withIfElse:Array<Int>  = [for  (i in 0...6) if (i % 2 == 0) i else 0];

        // WRONG
        var <error descr="Incompatible type: Array<Int> should be Array<String>">withIf:Array<String>  = [for  (i in 0...6) if (i % 2 == 0) i]</error>;
        var <error descr="Incompatible type: Array<Int> should be Array<String>">withIfElse:Array<String>  = [for  (i in 0...6) if (i % 2 == 0) i else 0]</error>;

    }

    function testGeneric<A>(it:Iterable<A>, f:(item:A) -> Bool) {
        //CORRECT
        var withGenerics:Array<A> = [ for (x in it) if (f(x)) x];

        // WRONG
        // TODO should show error
        var withGenerics:Array<String> = [ for (x in it) if (f(x)) x];
    }

}
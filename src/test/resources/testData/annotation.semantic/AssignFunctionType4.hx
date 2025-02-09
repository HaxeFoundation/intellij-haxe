package ;
class FunctionTypeAssignTest4 {
    static function explisitCasts() {
        // ARGUMENTS : Normal

        // CORRECT
        var castFromTest:A->Void = ArgA;
        var castFromTest:Int->Void = ArgA; // explicit from cast allowed

        // WRONG
        // error: Single should be A
        var <error descr="Incompatible type: A->Void should be Single->Void">castFromTest:Single->Void = ArgA</error>; // explicit to cast is wrong direction

            // WRONG
            //  error: String should be A
        var <error descr="Incompatible type: A->Void should be String->Void">castFromTest: String->Void = ArgA</error>; // implicit to/from cast not allowed



            // ARGUMENTS : typeParameters

            // CORRECT
        var castFromTest:Array<A>->Void = argTypeParameterA;

            // WRONG
            //error: Int should be A
        var <error descr="Incompatible type: Array<A>->Void should be Array<Int>->Void">castFromTest:Array<Int>->Void = argTypeParameterA</error>;

            // WRONG
            //  error: String should be A
        var <error descr="Incompatible type: Array<A>->Void should be Array<String>->Void">castFromTest:Array<String>->Void = argTypeParameterA</error>;

            // WRONG
            // error: Single should be A
        var <error descr="Incompatible type: Array<A>->Void should be Array<Single>->Void">castFromTest:Array<Single>->Void = argTypeParameterA</error>;


            //RETURN TYPES

            // CORRECT
        var castFromTest:Void->A = returnA;

            // CORRECT
        var castFromTest:Void->Array<A> = returnTypeParameterA;

            // WRONG
            // error: A should be Int
        var <error descr="Incompatible type: Void->A should be Void->Int">castFromTest:Void->Int = returnA</error>;

            // WRONG
            // error: A should be Int
        var <error descr="Incompatible type: Void->Array<A> should be Void->Array<Int>">castFromTest:Void->Array<Int>= returnTypeParameterA</error>;
    }


    static function argTypeParameterA(arg:Array<A>) {

    }

    static function returnTypeParameterA():Array<A> {
        return [1];
    }

    static function ArgA(arg:A) {

    }

    static function returnA():A {
        return 1;
    }
}
abstract A(Float) to Single from Int  {

    function new(f:Float) {
        this = f;
    }

    @:to
    function toString():String{
        return "";
    }

    @:from
    static function formString(s:String){
        return new A(1.0);
    }
}

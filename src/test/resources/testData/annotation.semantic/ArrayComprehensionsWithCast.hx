class ArrayComprehensionsWithCast {
    function test() {

        // NORMAL COMPREHENSIONS

        var normal1:Array<Int>  = [for  (i in 0...6) 0];
        var normal2:Array<Float>  = [for  (i in 0...6) 0.0];

        // COMPREHENSIONS WITH TYPE CAST

        // CORRECT : int has direct cast to float
        var castIntArray:Array<Float>  = [for  (i in 0...6) 0];

        // WRONG  : float does not have direct cast to Int and int does not have  direct cast from Float
        var castFloatArray:Array<Int> = <error descr="Incompatible type: Array<Float> should be Array<Int>">[for (i in 0...10) 0.0]</error>;
    }
}
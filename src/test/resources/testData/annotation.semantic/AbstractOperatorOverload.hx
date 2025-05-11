package ;
class AbstractOperatorOverload {
    public function new() {
        var noOverloads:AbsType = null;
        var overloads:AbsOperatorOverload = null;

        var intExpected:Int =  overloads - noOverloads; // should return Int
        var stringExpected:String =  overloads + noOverloads; // should return String

        var intExpected:Int =  overloads--; // should return Int
        var stringExpected:String =  overloads++; // should return String


        var intExpected:AbsType =  --overloads; // return AbsType
        var wrong =  --overloads;

        var intExpected:Int =  overloads - "1"; // CORRECT:  string got overload
        var stringExpected:String =  overloads + "1"; // CORRECT: string got overload



        var na =  <error descr="Unable to apply operator - for types AbsType and Int = 1">noOverloads - 1</error>; // WRONG: no overload
        var na =  <error descr="Unable to apply operator + for types AbsType and Int = 1">noOverloads + 1</error>; // WRONG: no overload

        var na =  <error descr="No overload for -- found">noOverloads--</error>; // WRONG: no overload
        var na =  <error descr="No overload for ++ found">noOverloads++</error>; // WRONG: no overload

        var wrong:Int =  <error descr="Incompatible type: String should be Int">overloads++</error>; // WRONG: returns String
        var wrong:String =  <error descr="Incompatible type: Int should be String">overloads--</error>; // WRONG:  returns Int

        var operatorTypeMismatch =  <error descr="Unable to apply operator - for types AbsOperatorOverload and Int = 1">overloads - 1</error>; // WRONG: no matching overload
        var operatorTypeMismatch =  <error descr="Unable to apply operator + for types AbsOperatorOverload and Int = 1">overloads + 1</error>; // WRONG: no matching overload

        var wrongType:Int =  <error descr="Incompatible type: AbsType should be Int">--overloads</error>; // WRONG: returns AbsType
        var missing =   <error descr="No overload for ++ found">++overloads</error>; // WRONG: no overload



    }
}

abstract AbsType(String) to String from String {}

abstract AbsOperatorOverload(String) to String from String {

    @:op(A++)
    function plusPlus(){
        return this + "opearator++";
    }

    @:op(A--)
    function minusMinus(){
        return 0;
    }

    @:op(--A)
    function minusMinusA():AbsType {
        return null;
    }

    @:op(A + B)
    function aPlusB(b:AbsType){
        return this + "opearator+";
    }

    @:op(A - B)
    function aMinusB(b:AbsType){
        return 0;
    }
}

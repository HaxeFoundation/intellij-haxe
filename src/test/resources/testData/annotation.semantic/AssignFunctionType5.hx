package ;
class FunctionTypeAssignTest5 {
    static function main() {
        functionTypesAsArgs();
    }
    static function functionTypesAsArgs() {
        var assignFunctionAbstractArg:(MyAbstract->Void)->Int = methodA; // CORRECT (original type)
        var assignFunctionAbstractArg:(Float->Void)->Int = methodA;// CORRECT (underlying type)
        var assignFunctionAbstractArg:(Single->Void)->Int = methodA; // CORRECT  (explicit to-cast type)

        var assignFunctionAbstractArg:(Int->Void)->Int = <error descr="Incompatible type: MyAbstract->Void->Int should be Int->Void->Int">methodA</error>;// WRONG   (int is a cast-from)
        var assignFunctionAbstractArg:(String->Void)->Int = <error descr="Incompatible type: MyAbstract->Void->Int should be String->Void->Int">methodA</error>; // WRONG (implicit cast not allowed)
        var assignFunctionAbstractArg:(Test-> Void)->Int = <error descr="Incompatible type: MyAbstract->Void->Int should be Test->Void->Int">methodA</error>; // WRONG  different type
    }


    static function methodA(arg:MyAbstract->Void) {
        return 1;
    }

}
abstract MyAbstract(Float) to Single from Int  {

    function new(f:Float) {
        this = f;
    }

    @:to
    function toString():String{
        return "";
    }

    @:from
    static function formString(s:String){
        return new MyAbstract(1.0);
    }
}

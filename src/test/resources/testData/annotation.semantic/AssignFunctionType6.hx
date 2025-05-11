package ;
class FunctionTypeAssignTest6 {
    static function main() {
        functionTypesAsArgs();
    }
    static function functionTypesAsArgs() {

        // superclass argument type
        var assignFunctionArg:(A->Void)->Int = methodA; // CORRECT

        var assignFunctionArg:(B->Void)->Int = <error descr="Incompatible type: A->Void->Int should be B->Void->Int">methodA</error>;// WRONG
        var assignFunctionArg:(C->Void)->Int = <error descr="Incompatible type: A->Void->Int should be C->Void->Int">methodA</error>; // WRONG
        var assignFunctionArg:(D->Void)->Int = <error descr="Incompatible type: A->Void->Int should be D->Void->Int">methodA</error>; // WRONG

        // subclass argument type
        var assignFunctionSubClassArg:(A->Void)->Int = methodB; // CORRECT
        var assignFunctionSubClassArg:(B->Void)->Int = methodB;// CORRECT
        var assignFunctionSubClassArg:(D->Void)->Int = methodB; // CORRECT
        var assignFunctionSubClassArg:(D->Void)->Float = methodB; // CORRECT

        var assignFunctionSubClassArg:(C->Void)->Int = <error descr="Incompatible type: B->Void->Int should be C->Void->Int">methodB</error>; // WRONG


        // interface argument type
        var assignFunctionInterfaceArg:(D->Void)->Int = methodD; // CORRECT

        var assignFunctionInterfaceArg:(A->Void)->Int = <error descr="Incompatible type: D->Void->Int should be A->Void->Int">methodD</error>; // WRONG
        var assignFunctionInterfaceArg:(B->Void)->Int = <error descr="Incompatible type: D->Void->Int should be B->Void->Int">methodD</error>;// WRONG
        var assignFunctionInterfaceArg:(C->Void)->Int = <error descr="Incompatible type: D->Void->Int should be C->Void->Int">methodD</error>; // WRONG

        // incorrect  return type


    }


    static function methodA(arg:A->Void) {
        return 1;
    }
    static function methodB(arg:B->Void) {
        return 1;
    }

    static function methodD(arg:D->Void) {
        return 1;
    }

}
class A {}
class B extends A implements D {}
class C extends B {}
interface D {}
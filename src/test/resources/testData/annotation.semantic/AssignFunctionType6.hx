package ;
class FunctionTypeAssignTest6 {
    static function main() {
        functionTypesAsArgs();
    }
    static function functionTypesAsArgs() {

        // superclass argument type
        var assignFunctionArg:(A->Void)->Int = methodA; // CORRECT

        var <error descr="Incompatible type: A->Void->Int should be B->Void->Int">assignFunctionArg:(B->Void)->Int = methodA</error>;// WRONG
        var <error descr="Incompatible type: A->Void->Int should be C->Void->Int">assignFunctionArg:(C->Void)->Int = methodA</error>; // WRONG
        var <error descr="Incompatible type: A->Void->Int should be D->Void->Int">assignFunctionArg:(D->Void)->Int = methodA</error>; // WRONG

            // subclass argument type
        var assignFunctionSubClassArg:(A->Void)->Int = methodB; // CORRECT
        var assignFunctionSubClassArg:(B->Void)->Int = methodB;// CORRECT
        var assignFunctionSubClassArg:(D->Void)->Int = methodB; // CORRECT
        var assignFunctionSubClassArg:(D->Void)->Float = methodB; // CORRECT

        var <error descr="Incompatible type: B->Void->Int should be C->Void->Int">assignFunctionSubClassArg:(C->Void)->Int = methodB</error>; // WRONG


            // interface argument type
        var assignFunctionInterfaceArg:(D->Void)->Int = methodD; // CORRECT

        var <error descr="Incompatible type: D->Void->Int should be A->Void->Int">assignFunctionInterfaceArg:(A->Void)->Int = methodD</error>; // WRONG
        var <error descr="Incompatible type: D->Void->Int should be B->Void->Int">assignFunctionInterfaceArg:(B->Void)->Int = methodD</error>;// WRONG
        var <error descr="Incompatible type: D->Void->Int should be C->Void->Int">assignFunctionInterfaceArg:(C->Void)->Int = methodD</error>; // WRONG

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
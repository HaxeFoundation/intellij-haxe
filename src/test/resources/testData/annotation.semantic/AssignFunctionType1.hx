package ;
class FunctionTypeAssignTest1 {

    static function testClassHierarchy() {
        // ARGUMENT TYPES

        //correct
        var b: B -> Void = fnWithArgB;
        var a: A -> Void = FnWithArgA;
        var b: B -> Void =  FnWithArgA;// NOTE: this is OK because B extends A and is therefor compatible

        // wrong
        //NOTE: the method assigned must accept type A, provide method can not
        var <error descr="Incompatible type: B->Void should be A->Void">a: A -> Void = fnWithArgB</error>;


        // RETURN TYPES

        // allowed as B is extending A and  A is expected
        var a: Void -> A = fnWithReturnB;

        // wrong Expects B but provided method might return A
        var <error descr="Incompatible type: Void->A should be Void->B">a: Void -> B = fnWithReturnA</error>;

    }

    static  function fnWithArgB(arg:B) {

    }
    static  function FnWithArgA(arg:A) {

    }
    static  function fnWithReturnB():B {
        return null;
    }
    static  function fnWithReturnA():A {
        return null;
    }
}
class A {}
class B extends A {}
package ;
class ValueExpressionMonomorph {
    public function ifCases() {

        // real type should be prioritized over dynamic
        var testVar1/*<# :String #>*/   = if (true) aDynamic() else aString(); // expect String
        var testVar2/*<# :String #>*/   = if (true) aString() else aDynamic(); // expect String
        var testVar3/*<# :String #>*/   = ifReturn(); // expect String

        // classTypes should be  unified
        var testVar4/*<# :B #>*/ =  if (true) classA() else classB(); // expect B
        var testVar5/*<# :B #>*/ =  if (true) classB() else classA(); // expect B

        //when  its not possible ot unify first occurens should be used (later occurences should show errors)
        var testVar6 =  if (true) aString() else classA(); // TODO expect String  + error on classA()
        var testVar7 =  if (true) classA() else aString(); // TODO expect A + error on aString()

    }

    function ifReturn() {
        return if(true) {
            if(1 == 1) aDynamic() else aString();
        }else {
            return aDynamic();
        }
    }
    public function switchCases(enum1:E<A> = DYNAMIC) {

        // real type should be prioritized over dynamic
        var testVar1/*<# :String #>*/ = switch (enum1) { // expect string
            case DYNAMIC : aDynamic();
            case STRING : aString();
            default : null;
        }

        var testVar2/*<# :String #>*/ = switch (enum1) { // expect string
            case STRING : aString();
            case DYNAMIC : aDynamic();
            default : null;
        }
        var testVar3a/*<# :String #>*/ = switchReturn(); // expect String
        var testVar3b/*<# :E<String> #>*/ = switchReturn2(); // expect E<String>

        // classTypes should be  unified
        var testVar4/*<# :B #>*/ =  switch (enum1) { // expect B
            case STRING : classA();
            case DYNAMIC : classB();
            default : null;
        }
        var testVar5/*<# :B #>*/ =  switch (enum1) { // expect B
            case CLASS(x) : x;
            case DYNAMIC : classB();
            case STRING : classA();
            default : null;
        }

        var testVar6 =  switch (enum1) { // TODO expect B + error on aString
            case CLASS(x) : x;
            case DYNAMIC : classB();
            case STRING : aString();
        }
        var testVar7 =  switch (enum1) { // TODO expect String + error on later values
            case STRING : aString();
            case CLASS(x) : x;
            case DYNAMIC : classB();
        }

    }

    function switchReturn(enum1:E<A> = DYNAMIC) {
        return switch (enum1) {
            case DYNAMIC : aDynamic();
            case STRING : aString();
        }
    }
    function switchReturn2(enum1:E<A> = DYNAMIC) {
        return switch (enum1) {
            case DYNAMIC : aDynamic();
            case STRING : CLASS("");
        }
    }

    function aDynamic():Dynamic {return null;}
    function aString():String {return null;}
    function classA():A {return null;}
    function classB():B {return null;}
}


class A extends B {function new() { super(); } }
class B extends C implements D {function new() {super();}}
class C {function new() {} }
interface  D {}
enum E<T> {
    DYNAMIC;
    STRING;
    CLASS(x:T);
}
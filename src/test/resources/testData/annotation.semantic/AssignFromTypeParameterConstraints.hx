package ;

class NullTypeReturnsTest {
    public function new() {
        // should work A extends B that implements D
        var a:A =  nullWrappedReturn();
        var a:A = normalReturn();
        var a:Array<A> =  nullWrappedTypeParameterReturn();

        // should work  B implements D
        var b:B = nullWrappedReturn();
        var b:B = normalReturn();
        var a:Array<B> =  nullWrappedTypeParameterReturn();

        //TODO errors should probably just show constraint

        // should fail, C does not implement interface D
        var <error descr="Incompatible type: Null<T:D> should be C">c:C = nullWrappedReturn()</error>;
        var <error descr="Incompatible type: T:D should be C">c:C = normalReturn()</error>;
        var <error descr="Incompatible type: Array<Null<T:D>> should be Array<C>">a:Array<C> =  nullWrappedTypeParameterReturn()</error><EOLError descr="Missing semicolon."></EOLError>

        // wrong does not match constraints
        var <error descr="Incompatible type: T:D should be String">c:String = normalReturn()</error>;
    }
    public function nullWrappedReturn<T:D>():Null<T> {return null;}
    public function normalReturn<T:D>():T {return null;}
    public function nullWrappedTypeParameterReturn<T:D>():Array<Null<T>> {return null;}
    public function nullWrappedTypeParameterReturn2<T:D>():Array<Null<String>> {return null;}
}

class A extends B {function new() { super(); } }
class B extends C implements D {function new() {super();}}
class C {function new() {} }
interface  D {}

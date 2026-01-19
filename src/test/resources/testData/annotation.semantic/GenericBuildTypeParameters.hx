@:genericBuild(<warning descr="Unresolved symbol">TupleMacro</warning>.<warning descr="Unresolved symbol">build</warning>())
class TupleClass<Rest> {
    public function new() {}
}

@:genericBuild(<warning descr="Unresolved symbol">MixedMacro</warning>.<warning descr="Unresolved symbol">build</warning>())
class MixedClass<T, Rest> {
    public var test:T;
    public function new() {}
}

@:genericBuild(<warning descr="Unresolved symbol">ConstMacro</warning>.<warning descr="Unresolved symbol">build</warning>())
class ConstClass<Const> {
    public function new() {}
}

class GenericBuildMacro {
    public function testTupple() {
        var test1:TupleClass<Int, String> = new TupleClass<Int, String>();
        var test2 = new TupleClass<Int, String>();
        var test3:TupleClass<Int, String> = new TupleClass( );
        var test4:TupleClass<[Int, String]> = new TupleClass();
        var test5 = new TupleClass();

        // TODO mlo:not sure if zero typeParameters should be allowed or not
        var error1:<error descr="Invalid number of type parameters for TupleClass (expected: 1 got: 0)">TupleClass</error> = new TupleClass();
    }
    public function testAssignTupples() {
        var valueA= new TupleClass<Int, String>();

        var Ok:TupleClass<Int,  String> = valueA; //  correct typeParameter list

        var wrong1:TupleClass<String,  String> = <error descr="Incompatible type: TupleClass<Int, String> should be TupleClass<String, String>">valueA</error>; //  wrong typeParameter
        var wrong2:TupleClass<Int,  String, Bool> = <error descr="Incompatible type: TupleClass<Int, String> should be TupleClass<Int, String, Bool>">valueA</error>; // missing typeParameter
        var wrong2:MixedClass<Int,  String> = <error descr="Incompatible type: TupleClass<Int, String> should be MixedClass<Int, String>">valueA</error>; // wrong type
    }

    public function testMixed() {
        var test1:MixedClass<Int, Bool, String> = new MixedClass<Int, Bool, String>();
        var test2 = new MixedClass<Int, String>();
        var test3:MixedClass<Int, Bool, String> = new MixedClass();
        var test4:MixedClass<Int, [Bool, String]> = new MixedClass();
        var test5= new MixedClass();

        var testNonRestTypeParameter = test1.test * 2;


        // TODO mlo: not sure if zero values for Rest typeParameter should be allowed or not
        var error1:<error descr="Invalid number of type parameters for MixedClass (expected: 2 got: 1)">MixedClass<Int></error> = <error descr="Incompatible type: MixedClass<Int, Rest> should be MixedClass<Int>">new MixedClass()</error>;
    }

    public function testConst() {
        var test1:ConstClass<"data.hx"> = new ConstClass<"data.hx">();
        var test2 = new ConstClass<"data.hx">();
        var test3:ConstClass<"data.hx"> = new ConstClass();
        var test4 = new ConstClass(); // not sure if this makes sense ?

        var test5 = new ConstClass<1>();
        var test6 = new ConstClass<false>();

        // TODO mlo: find out if const fields (inline, enums etc) should be allowed/suppored
    }
}

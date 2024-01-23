package ;


interface ISomeInterface<T> {
    function myMethod(param:T):String;
}

typedef StructureWithTypeParameters <T, Q> = { fistVar:T, secondVar:Q}
typedef SpecifiedStrcuture = StructureWithTypeParameters<String, Int>;

typedef FunctionTypeWithTypeParameters<T, Q> = T -> Q;
typedef SpecifiedFunction = FunctionTypeWithTypeParameters<Int, String>;

class Test {

    // tests that we can resolve type from annonymous structure  used as type parameter
    function anonStructVarInTypeParam<T:{a:String}>(parameter:T) {
        var s:String =  parameter.a.toLowerCase(); // correct
        var i:Int = <error descr="Unable to apply operator * for types String and Int = 2">parameter.a * 2</error>; // wrong
    }
    // tests that we can resolve type from annonymous function  used as type parameter
    function anonStructFnInTypeParam<T:{a:Int->String}>(parameter:T) {
        var s:String =  parameter.a(1); // correct
        var s:String =  parameter.a(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"1"</error>); // wrong
        var i:Int = <error descr="Unable to apply operator * for types Int->String and Int = 2">parameter.a * 2</error>; // wrong
    }

    // tests that we can resolve type from annonymous structure defined in typedef
    function test3(parameter:SpecifiedStrcuture) {
        var i:Int  = parameter.fistVar.length; // correct
        var j:Int  = parameter.secondVar * 2; // correct

        var k:Int = <error descr="Unable to apply operator * for types String and Int = 2">parameter.fistVar * 2</error>; // wrong
    }

    function test<T:(SpecifiedStrcuture, ISomeInterface<String>)>(target:T):Void {
        var s:String = target.myMethod(""); // correct from interface
        var t:String = target.fistVar.toLowerCase(); // correct from  SpecifiedStrcuture

        var s:String = target.myMethod(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // wrong parameter type
        var t:Int = <error descr="Unable to apply operator * for types T and Int = 2">target.fistVar * 2</error>; // wrong

    }


    function test4(x:SpecifiedFunction) {
        var x1:String = x(1); // correct

        var wrong:String = x(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"A"</error>);// wrong
        var <error descr="Incompatible type: String should be Int">x1:Int =  x(1)</error> ; // wrong

    }
}

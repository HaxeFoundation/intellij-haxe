package ;
using StringTools;

import  StringBuf as ImportAlias;

typedef MyStruct = {a:String, b:Int}
class A {}
class B extends A {}
class C implements I {}
interface I {}


class NoArgs { function new () {}}
class OneArgs{ function new (arg1:String) {}}
class ClassArgs{ function new (arg1:Class<String>) {}}
class OptionalArgs{ function new (arg1:String, ?arg2:Int) {}}
class DefaultArgs{ function new (arg1:String, arg2:String = "") {}}
class FunctionArgs{ function new (arg1:String,  arg3:Int->String) {}}
class FunctionArgs2{ function new (arg1:String,  arg3:(String->Int)->(Int->String)->Float) {}}
class VarArgs{ function new (arg1:String, extra:Array<haxe.macro.Expr>) {}}
class RestArgs{ function new (arg1:String, extra:haxe.extern.Rest<String>) {}}
class RestSyntaxArgs{ function new (arg1:String, ...extra:String) {}}
class TypeDefArg{ function new (arg1:MyStruct) {}}
class ClassInheritArgs{ function new (arg1:A) {}}
class InterfaceInheritArgs{ function new (arg1:C) {}}
class GenericArgs<T>{ function new (arg1:T, Arg2:T):T {}}
class GenericClassArgs<T>{ function new (arg1:Class<T>):T {}}
class GenericConstraintsArgs<T:String>{ function new (arg1:T):T {}}
class GenericClassConstraintsArgs<T:A>{ function new (arg1:Class<T>):T {}}
class GenericComplexConstraintsArgs<T:String>{ function new (arg1:Array<T>) {} }


class Test {
    public function test() {

        " Test static extension ".contains("test");// CORRECT
        " Test static extension ".contains(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // WRONG expected arg is string

        new NoArgs(); // CORRECT
        <error descr="Too many arguments (expected 0 but got 1)\"">new NoArgs("String")</error>; // WRONG (no arg expected got one)

        new OneArgs("String"); // CORRECT
        new OneArgs(null); //CORRECT (String can be null)

        new OneArgs(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // WRONG (incorrect argument type)
        new OneArgs<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; // WRONG (Missing argument)

        new ClassArgs(String); // correct
        new ClassArgs(<error descr="Type mismatch (Expected: 'Class<String>' got: 'String')">"str"</error>); // wrong

        new OptionalArgs("String"); // CORRECT (optional is not required)
        new OptionalArgs("String", 1); // CORRECT (optional can be set)

        new OptionalArgs<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; // WRONG (Missing first argument)

        new DefaultArgs("String"); // CORRECT (default is used)
        new DefaultArgs("String", "String2"); // CORRECT (default can be overriden)

        new DefaultArgs<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; // WRONG (Missing first argument)

        new FunctionArgs("FunctionA", (myInt)->{return "String";}); // CORRECT
        new FunctionArgs("FunctionA", (a:Int)->{return "String";}); // CORRECT
        new FunctionArgs("FunctionA", intToString); // CORRECT


        new FunctionArgs("FunctionA", <error descr="Type mismatch (Expected: 'Int->String' got: 'String')">{return "String";}</error>); // WRONG function must accept Int parameter
        new FunctionArgs("FunctionA", <error descr="Type mismatch (Expected: 'Int->String' got: 'Int')">{return 1;}</error>); // WRONG function must return String
        new FunctionArgs("FunctionA", <error descr="Type mismatch (Expected: 'Int->String' got: 'Int')">1</error>); // WRONG argument type is not a function
        new FunctionArgs("FunctionA", <error descr="Type mismatch (Expected: 'Int->String' got: 'String->Int')">stringToInt</error>); // WRONG

        var sToI:String->Int;
        var iToS:Int->String;
        new FunctionArgs2("FunctionA", (sToI,iToS) -> 1.0 ); // CORRECT

        new VarArgs("Stirng1", "String2", "String3", "String4 "); //CORRECT
        new VarArgs("Stirng1"); //CORRECT ( when using varArg, arguments are optional)

        new VarArgs<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; //WRONG  normal arguments are still required

        new RestArgs("Stirng1", "String2", "String3", "String4"); //CORRECT
        new RestArgs("Stirng1"); //CORRECT ( when using Rest, arguments are optional)

        new RestArgs<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; //WRONG  normal arguments are still required

        new RestSyntaxArgs("Stirng1", "String2", "String3", "String4"); //CORRECT
        new RestSyntaxArgs("Stirng1"); //CORRECT ( when using Rest, arguments are optional)

        new RestSyntaxArgs<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; //WRONG  normal arguments are still required

        new ClassInheritArgs(new A()); // CORRECT
        new ClassInheritArgs(new B() ); // CORRECT B extends A

        new ClassInheritArgs(<error descr="Type mismatch (Expected: 'A' got: 'C')">new C()</error>); // WRONG C has no relation to A

        new InterfaceInheritArgs(new C()); // CORRECT  C implements I

        new InterfaceInheritArgs(<error descr="Type mismatch (Expected: 'C' got: 'B')">new B()</error>); // WRONG B does not implement I

        new TypeDefArg(new MyStruct());

        new GenericArgs(1,2); // CORRECT both args are of same type

        new GenericArgs(1, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"2"</error>); // WRONG  type missmatch

        new GenericConstraintsArgs("Str"); // CORRECT  type must be or extend string

        new GenericConstraintsArgs(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // WRONG   genric type constraints does not allow type

        new GenericComplexConstraintsArgs([""] ); // CORRECT Array of T (where T is String)

        new GenericComplexConstraintsArgs(<error descr="Type mismatch (Expected: 'Array<String>' got: 'Array<Int>')">[1]</error> ); // WRONG Array of T , T not matching constraints

        var myMap:Map<String, Int> = new Map();
        myMap.set("", 1); //CORRECT :  argument types matches Type parameters
        myMap.set(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>, <error descr="Type mismatch (Expected: 'Int' got: 'String')">""</error>); //WRONG : argument types does not match Type parameters


        var instance:StringBuf;

        new GenericClassArgs(String);  // WRONG parameter type  (should be Class)

        new GenericClassArgs(<error descr="Type mismatch (Expected: 'Class<T>' got: 'Int')">1</error>);  // WRONG parameter type  (should be Class)
        new GenericClassArgs(<error descr="Type mismatch (Expected: 'Class<T>' got: 'StringBuf')">instance</error>);  // WRONG parameter type (should be Class)

        new GenericClassConstraintsArgs(A); // CORRECT (matches constraints)
        new GenericClassConstraintsArgs(B); // CORRECT since B extends A

        new GenericClassConstraintsArgs(<error descr="Type mismatch (Expected: 'Class<A>' got: 'Class<C>')">C</error>); // WRONG  type does not match constraint
        new GenericClassConstraintsArgs(<error descr="Type mismatch (Expected: 'Class<A>' got: 'Int')">1</error>); // WRONG  type does not match constraint

    }

    function intToString(i:Int):String {
        return "";
    }

    function stringToInt(s:String):Int {
        return 1;
    }

}
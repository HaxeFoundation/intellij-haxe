package ;
using StringTools;

typedef MyStruct = {a:String, b:Int}
class A {}
class B extends A {}
class C implements I {}
interface I {}

class CallExpressionTest {
    function noArgs() {}
    function oneArgs(arg1:String) {}
    function optionalArgs(arg1:String, ?arg2:Int) {}
    function defaultArgs(arg1:String, arg2:String = "") {}
    function functionArgs(arg1:String,  arg3:Int->String) {}
    function varArgs(arg1:String, extra:Array<haxe.macro.Expr>) {}
    function restArgs(arg1:String, extra:haxe.extern.Rest<String>) {}
    function typeDefArg(arg1:MyStruct) {}
    function classInheritArgs(arg1:A) {}
    function interfaceInheritArgs(arg1:C) {}


    public function new() {

        " Test static extension ".contains("test");// CORRECT
        " Test static extension ".contains(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // WRONG expected arg is string

        noArgs(); // CORRECT
        noArgs(<error descr="Too many arguments (expected 0 but got 1)\"">"String"</error>); // WRONG (no arg expected got one)

        oneArgs("String"); // CORRECT
        oneArgs(null); //CORRECT (String can be null)
        oneArgs(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>); // WRONG (incorrect argument type)
        <error descr="Too many arguments (expected 1 but got 0)\"">oneArgs</error>(); // WRONG (Missing argument)

        optionalArgs("String"); // CORRECT (optional is not required)
        optionalArgs("String", 1); // CORRECT (optional can be set)
        <error descr="Too many arguments (expected 1 but got 0)\"">optionalArgs</error>(); // WRONG (Missing first argument)

        defaultArgs("String"); // CORRECT (default is used)
        defaultArgs("String", "String2"); // CORRECT (default can be overriden)
        <error descr="Too many arguments (expected 1 but got 0)\"">defaultArgs</error>(); // WRONG (Missing first argument)

        functionArgs("FunctionA", (myInt)->{return "String";}); // CORRECT
        functionArgs("FunctionA", (a:Int)->{return "String";}); // CORRECT
        functionArgs("FunctionA", <error descr="Type mismatch (Expected: 'Int->String' got: 'String->String')">(a:String)->{return "String";}</error>); // WRONG function must accept Int parameter
        functionArgs("FunctionA", <error descr="Type mismatch (Expected: 'Int->String' got: 'Int->Int')">(a:Int)->{return 1;}</error>); // WRONG function must return String
        functionArgs("FunctionA", <error descr="Type mismatch (Expected: 'Int->String' got: 'Int')">1</error>); // WRONG argument type is not a function

        varArgs("Stirng1", "String2", "String3", "String4 "); //CORRECT
        varArgs("Stirng1"); //CORRECT ( when using varArg, arguments are optional)
        <error descr="Too many arguments (expected 1 but got 0)\"">varArgs</error>(); //WRONG  normal arguments are still required

        restArgs("Stirng1", "String2", "String3", "String4"); //CORRECT
        restArgs("Stirng1"); //CORRECT ( when using Rest, arguments are optional)
        <error descr="Too many arguments (expected 1 but got 0)\"">restArgs</error>(); //WRONG  normal arguments are still required

        classInheritArgs(new A()); // CORRECT
        classInheritArgs(new B() ); // CORRECT B extends A
        classInheritArgs(<error descr="Type mismatch (Expected: 'A' got: 'C')">new C()</error>); // WRONG C has no relation to A

        interfaceInheritArgs(new C()); // CORRECT  C implements I
        interfaceInheritArgs(<error descr="Type mismatch (Expected: 'C' got: 'B')">new B()</error>); // WRONG B does not implement I

        typeDefArg(new MyStruct());

        var myMap:Map<String, Int> = new Map();
        myMap.set("", 1); //CORRECT :  argument types matches Type parameters
        myMap.set(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>, <error descr="Type mismatch (Expected: 'Int' got: 'String')">""</error>); //WRONG : argument types does not match Type parameters

    }
}

package ;

interface  BaseInterface{function testA(i:Int):String;}
class  Baseclass implements BaseInterface {
    public function testA(i:Int):String;
    public function testB(i:String):String;
}

class  OtherClass {}

class Generics {
    public function new() {

        // verify that we get expected class(Baseclass) and not just constraints(BaseInterface)
        var assignA:Baseclass = getValue(Baseclass);
        var assignB:BaseInterface = getValue(Baseclass);
        var assignB:BaseInterface = getValue(<error descr="Type mismatch (Expected: 'Class<BaseInterface>' got: 'Class<OtherClass>')">OtherClass</error>);// wrong: does not match constraints
        var <error descr="Incompatible type: BaseInterface should be Baseclass">assignC:Baseclass = getValue(BaseInterface)</error>; //wrong

        // verify that correct type is resolved when no typetag is provided
        var notTypeTag1 = getValue(Baseclass);
        var test1Return = notTypeTag1.testB(""); // correct: should find TestB from Baseclass
        test1Return.toLowerCase(); // correct: should resolve to string

        var <error descr="Incompatible type: String should be Int">bad:Int = notTypeTag1.testB(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>)</error>;

        var notTypeTag2 = getValue(BaseInterface);
        var test2Return = notTypeTag2.testA(1); // correct

        // verify that "chained" expressions work
        getValue(BaseInterface).testA(1); // correct
        getValue(Baseclass).testB("1"); // correct
        getValue(BaseInterface).<warning descr="Unresolved symbol">testB</warning>("1"); // Wrong BaseInterface does not ccontain

        var test2Return = notTypeTag2.testA(<error descr="Type mismatch (Expected: 'Int' got: 'String')">""</error>); // wrong: incorrect argument
        var test2Return = notTypeTag2.<warning descr="Unresolved symbol">testB</warning>(""); // wrong: should not find testB
        var test2Return = notTypeTag2.<warning descr="Unresolved symbol">testC</warning>(""); // wrong: does not exsist

        var passTypeParamToResult  =  haxe.ds.Vector.fromArrayCopy(["A","B"]);
        passTypeParamToResult.get(0).toLowerCase();// correct: should get should return a string as T is string

        var chainedTypeParameter = new Array<Array<String>>();
        chainedTypeParameter[0][0].toLowerCase();// correct should find String

    }

    function getValue<T:BaseInterface>(value:Class<T>):T {
        return null;
    }
}

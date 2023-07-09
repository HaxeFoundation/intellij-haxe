package ;
class RestArgumentsTest {

    static function main() {
        var no:Int;
        var str:String;
        var fn:Int->String;
        var ano:{i:Int};

        stringRestArgs(str, str, str);
        functionRestArgs( fn, fn, fn );
        anonymousRestArgs(ano, ano,ano);


        stringRestArgs(<error descr="Type mismatch (Expected: 'String' got: 'Int')">no</error>, <error descr="Type mismatch (Expected: 'String' got: 'Int')">no</error>, <error descr="Type mismatch (Expected: 'String' got: 'Int')">no</error>); // WRONG type


    }

      static function restArgsForward(...restArray:String)
      {
          var x = "Str";
          var arr:Array<String> = ["argA", "ArgB"];

          stringRestArgs(...restArray); // CORRECT
          stringRestArgs(x, ...restArray); // CORRECT
          stringRestArgs(x, ...arr); // CORRECT
          stringRestArgs(x, ...["arg1", "arg2"]); // CORRECT

          stringRestArgs(...restArray<error descr="',' unexpected">,</error> x); // WRONG spread operator argument has to be the last argument
          functionRestArgs(<error descr="Type mismatch (Expected: 'Int->String' got: 'String')">...restArray</error>); // WRONG  type mismatch

      }

    static function stringRestArgs(...restArray:String)
    {
        var element:String = restArray[0]; //CORRECT
        var chainResult:String = restArray[0].charAt(0); //CORRECT

        var <error descr="Incompatible type: String should be Int">wrongElement:Int = restArray[0]</error>; //WRONG (type missmatch)
        var wrongChainResult:Int = <error descr="Unable to apply operator * for types String and Int = 2">restArray[0] * 2</error>; //WRONG (type missmatch)

    }

    static function functionRestArgs(...restArray:Int->String)
    {
        var element:Int->String = restArray[0]; //CORRECT
        var chainResult:String = restArray[0](0); //CORRECT

        var <error descr="Incompatible type: Int->String should be String">wrongElement:String = restArray[0]</error>; //WRONG (type missmatch)

    }
    static function anonymousRestArgs(...restArray:{i:Int})
    {
        var element:{i:Int}= restArray[0]; //CORRECT
        var chainResult:Int = restArray[0].i; //CORRECT


        var <error descr="Incompatible type: {i:Int} should be String">wrongElement:String = restArray[0]</error>; //WRONG (type missmatch)

    }
}

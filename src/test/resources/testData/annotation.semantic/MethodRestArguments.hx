package ;
class RestArgumentsTest {

    static function main() {
        var no:Int;
        var str:String;
        var fn:Int->Void;
        var ano:{i:Int};

        stringRestArgs(str, str, str);
        functionRestArgs( fn, fn, fn );
        anonymousRestArgs(ano, ano,ano);

        //TODO type checking
        stringRestArgs(no, no, no); // WRONG
        //...

    }

    static function stringRestArgs(...restArray:String)
    {
        var element:String = restArray[0]; //CORRECT
        var chainResult:String = restArray[0].charAt(0); //CORRECT

        var <error descr="Incompatible type: String should be Int">wrongElement:Int = restArray[0]</error>; //WRONG (type missmatch)
        //TODO ?
        var wrongChainResult:Int = restArray[0] * 2; //WRONG (type missmatch)

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

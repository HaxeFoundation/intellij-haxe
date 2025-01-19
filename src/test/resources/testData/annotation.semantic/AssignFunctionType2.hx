package ;
class FunctionTypeAssignTest2 {
    static function testDynamicAndNull() {
        // ARGUMENTS: Normal types

        // Correct
        var acceptFromDynamicArg:Null<String> -> Void = dynamicArg;
        var acceptNullWrappedArg:Null<String> -> Void = stringArg;
        var acceptNullWrappedArg:Null<Dynamic> -> Void = dynamicArg;

        // ????
        // The compiler wont complain on most targets, guessing some kind of monomprph
        // tested on try.haxe.org with integer as input on different targets have different behaivior on this one.
        // --
        // neko: Invalid field access
        // javascript: undefined
        // eval: Uncaught exception
        // hashlink : Build failure
        //--
        var unexpected:Null<Dynamic> -> Void = stringArg;


        // ARGUMENTS: TypeParameters

        // Correct
        var acceptFromTpDynamic:Array<Null<String>> -> Void = dynamicTpArg;
        var acceptTpNullWrapped:Array<Null<String>> -> Void = stringTpArg;

        var assignToDynamicTp:Array<Null<Dynamic>> -> Void = dynamicTpArg;

        // Wrong :
        // provided method can not accept argument that has typeParameter of type dynamic
        var <error descr="Incompatible type: Array<String>->Void should be Array<Null<Dynamic>>->Void">assignToDynamicTp:Array<Null<Dynamic>> -> Void = stringTpArg</error>;



        // RETURN TYPES

        // Return: Normal types

        // Correct
        var acceptNullWrapped:Void -> Null<Dynamic>  = dynamicReturn;
        var acceptDynamicReturn: Void -> Null<String> = dynamicReturn;
        var nullWrappedReturn:Void -> Null<String>  = stringReturn;
        var nullWrappedDynReturn:Void -> Null<Dynamic>  = stringReturn;

        var acceptReturnNullWrapped1:Void -> String = nullDynamicReturn;
        var acceptReturnNullWrapped2:Void -> String  = nullStringReturn;
        var acceptReturnNullWrapped3:Void -> Dynamic  = nullDynamicReturn;

        //Wrong
        var <error descr="Incompatible type: Void->String should be Void->Null<Int>">wrong:Void -> Null<Int>  = stringReturn</error>;

        //????
        // allowed by compiler (monomprph?)
        var unexpected:Void -> Null<Int>  = dynamicReturn;


        // Return: TypeParameters

        // Correct
        var acceptNullWrapped:Void -> Array<Null<String>>  = stringTpReturn;
        var assignToDynamicTp:Void -> Array<Null<Dynamic>>  = dynamicTpReturn;
        var assignToDynamicTp:Void -> Array<Null<Dynamic>>  = stringTpReturn;

        // wrong: return array may contain anny thing and function type promisses only array of strings
        var <error descr="Incompatible type: Void->Array<Dynamic> should be Void->Array<Null<String>>">wrong: Void -> Array<Null<String>> = dynamicTpReturn</error>;


    }

    static function dynamicArg(arg:Dynamic) {}
    static function stringArg(arg:String) {trace(arg.length);}
    static function dynamicTpArg(arg:Array<Dynamic>) {trace(arg.length); }
    static function stringTpArg(arg:Array<String>) {trace(arg.length); }


    static function dynamicReturn():Dynamic {return null;}
    static function stringReturn():String {return null;}

    static function nullDynamicReturn():Null<Dynamic> {return null;}
    static function nullStringReturn():Null<String> {return null;}

    static function dynamicTpReturn():Array<Dynamic> {return null;}
    static function stringTpReturn():Array<String> {return null;}
}

class FunctionBindTest2 {
    public function new() {

// NORMAL ARGUMENTS
        // correct
        var bind:(Int, Float) -> String =  normal.bind( ); // bind has default behaivior for "missing" values
        var bind:Float -> String = normal.bind(1, _);
        var bind:Int -> String = normal.bind(_, 1);
        var bind:Void -> String = normal.bind(1, 1);

        // wrong
        var bind:Float -> String = normal.bind(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"1"</error>, _); // String should be Int
        var <error descr="Incompatible type: Int->String should be Float->String">bind:Float -> String = normal.bind(_, 1)</error>;  // Float should be Int

//OPTIONAL ARGUMENTS

            // correct
        var bind:Void -> String = optionalArgs.bind(1);
        var bind:Void -> String = optionalArgs.bind();
        var bind:Void -> String = optionalArgs.bind(1, 1);

            // wrong
        var bind:Float -> String = optionalArgs.bind(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"1"</error>, _); // String should be Int
        var bind:Float -> String = optionalArgs.bind(<error descr="Too many arguments (expected 2 but got 3)\"">1, _, 1</error>); // Too many callback arguments
        var <error descr="Incompatible type: Void->String should be Float->String">bind:Float -> String = optionalArgs.bind(<error descr="Too many arguments (expected 2 but got 3)\"">1, 1, 1</error>)</error>; // Too many callback arguments & wrong type


// VARARGS

        // correct
        var bind:...Float -> String = varargs.bind( 1, _);
        var bind:(...Float) -> String = varargs.bind(1, _);
        var bind:haxe.Rest<Float > -> String  = varargs.bind( 1 , _);
        var bind:Int -> String = varargs.bind(_,  [1.0,  2.0, 3.0]);
        var bind = varargs.bind(_, [1, 2, 3]);

        //wrong
        var bind = varargs.bind(1, <error descr="Type mismatch (Expected: 'haxe.Rest<Float>' got: 'String')">"string"</error>); // String should be haxe.Rest<Float>
        var bind = varargs.bind(<error descr="Too many arguments (expected 2 but got 3)\"">1, _, _</error> ); // Too many callback arguments

    }

    function normal(x:Int, y:Float):String {return null;}
    function optionalArgs(?x:Int, ?y:Float):String {return null;}
    function varargs(x:Int, ...y:Float):String {return null;}
}

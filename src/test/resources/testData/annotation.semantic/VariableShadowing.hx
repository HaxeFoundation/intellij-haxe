package ;



class Test {
  function test(x:Array<Int>) {
    x.push(1);

    // shadowing Parameter
    var x = "String";
    x.substr(1,2);

    // shadowing previous var
    var x = 1;
    x<<x;

    var x:Float = 1.0;

    // verify that previous var can be used in init of new shadow variable (intentional type mismatch)
    // the following line should fail (last  var x is float, map expects String)
    var <error descr="Incompatible type: haxe.ds.Map<String, Float> should be Map<String, String>">x:Map<String,String> = ["" => x]</error>;
    var x = x.get("");
  }
}


package;

class AbstractAssignmentFromTo1 {
  public static function variableTests():Void {
    var val:FunctionFromTo =  function(i:Int){return;};
    var val:FunctionFrom =  function(i:Int){return;};

    var val:FunctionFromTo =  testMethodIV;
    var val:FunctionFrom =  testMethodIV;

    // should fail due to wrong parameter types
    var <error descr="Incompatible type: String->Int should be FunctionFromTo">val:FunctionFromTo =  function(i:String){return 1;}</error>;
    var <error descr="Incompatible type: String->Int should be FunctionFrom">val:FunctionFrom =  function(i:String){return 1;}</error>;
    var <error descr="Incompatible type: String->Void should be FunctionFromTo">val:FunctionFromTo =  testMethodSV</error>;

    //should fail (has no from type or implisit casts)
    var <error descr="Incompatible type: Int->Void should be FunctionTo">val:FunctionTo =  function(i:Int){return;}</error>;
    var <error descr="Incompatible type: Int->Void should be FunctionTo">val:FunctionTo =  testMethodIV</error>;
    var <error descr="Incompatible type: Int->Void should be FunctionNon">val:FunctionNon =  testMethodIV</error>;

  }

  public static function testMethodIV(i:Int) {return;}
  public static function testMethodSV(i:String) {return;}
}



abstract FunctionFromTo(Int->Void) from Int->Void to Int->Void {}

abstract FunctionFrom(Int->Void) from Int->Void {}

abstract FunctionTo(Int->Void) to Int->Void {}

abstract FunctionNon(Int->Void) {}

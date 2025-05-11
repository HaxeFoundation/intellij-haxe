package;

class AbstractAssignmentFromTo1 {
  public static function variableTests():Void {
    var val:FunctionFromTo =  function(i:Int){return;};
    var val:FunctionFrom =  function(i:Int){return;};

    var val:FunctionFromTo =  testMethodIV;
    var val:FunctionFrom =  testMethodIV;

    // should fail due to wrong parameter types
    var val:FunctionFromTo =  <error descr="Incompatible type: String->Int should be FunctionFromTo">function(i:String){return 1;}</error>;
    var val:FunctionFrom =  <error descr="Incompatible type: String->Int should be FunctionFrom">function(i:String){return 1;}</error>;
    var val:FunctionFromTo =  <error descr="Incompatible type: String->Void should be FunctionFromTo">testMethodSV</error>;

    //should fail (has no from type or implisit casts)
    var val:FunctionTo =  <error descr="Incompatible type: Int->Void should be FunctionTo">function(i:Int){return;}</error>;
    var val:FunctionTo =  <error descr="Incompatible type: Int->Void should be FunctionTo">testMethodIV</error>;
    var val:FunctionNon =  <error descr="Incompatible type: Int->Void should be FunctionNon">testMethodIV</error>;

  }

  public static function testMethodIV(i:Int) {return;}
  public static function testMethodSV(i:String) {return;}
}



abstract FunctionFromTo(Int->Void) from Int->Void to Int->Void {}

abstract FunctionFrom(Int->Void) from Int->Void {}

abstract FunctionTo(Int->Void) to Int->Void {}

abstract FunctionNon(Int->Void) {}

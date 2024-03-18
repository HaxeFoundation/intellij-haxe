
class OptionalFieldsSyntax {
 public function new(?optionalArgument:String) {
    var callbackWithRequired:String->Void;
    var callbackWithOptional:?String->Void;

    callbackWithRequired = optionalMarkArg;
    callbackWithRequired = optionalArg;
    callbackWithRequired = reqArg;

    callbackWithRequired = function(s:String):Void{};
    callbackWithOptional = function(s:String = ""):Void{};
    callbackWithOptional = function(?s:String):Void{};

    // wrong argument type
    callbackWithRequired = <error descr="Incompatible type: ?Int->Void should be String->Void">optionalOtherArg</error>;
    callbackWithRequired = <error descr="Incompatible type: Int->Void should be String->Void">otherArg</error>;

    callbackWithOptional = optionalMarkArg;
    callbackWithOptional = optionalArg;

    //Optional parameters can't be forced
    callbackWithOptional = <error descr="Incompatible type: String->Void should be ?String->Void">reqArg</error>;
    callbackWithOptional = <error descr="Incompatible type: String->Void should be ?String->Void">function(s:String):Void{}</error>;
    // wrong argument type
    callbackWithOptional = <error descr="Incompatible type: ?Int->Void should be ?String->Void">optionalOtherArg</error>;
    callbackWithOptional = <error descr="Incompatible type: Int->Void should be ?String->Void">otherArg</error>;

 }

  public function optionalMarkArg(?arg:String):Void {}
  public function optionalArg(arg:String = null):Void {}
  public function optionalOtherArg(?arg:Int):Void {}
  public function reqArg(arg:String):Void {}
  public function otherArg(arg:Int):Void {}
}
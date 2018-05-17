package ;

class NewFunctionTypeSyntax {
  public static function main() {
    var simple:(arg0:Int) -> Void;
    var simpleNoArguments:() -> Void;
    var simpleArgumentsList:(arg0:Int, arg1:Bool) -> Void;
    var simpleArgumentsListWithoutName:(arg0:Int, Bool) -> Void;
    var simpleArgumentsListWithoutNameOptional:(arg0:Int, ?Bool) -> Void;

    var nestedFunctionTypeNoArguments:(arg0:Int, () -> Void) -> Void;
    var nestedFunctionTypeWithArguments1:(arg0:Int, (arg0:String) -> Void) -> Void;
    var nestedFunctionTypeWithArguments2:(arg0:Int, arg2:(arg0:String, Bool) -> Void) -> Void;
    var nestedFunctionTypeWithArguments3:(arg0:Int, arg2:(arg0:String, ?Bool) -> Void) -> Void;
    var optionalNestedFunctionType:(arg0:Int, ?(() -> Void)) -> Void;

    var nestedFunctionTypeWithOptionalArgument:(arg0:Int, (?arg0:String, ?Bool) -> Void) -> Void;
    var optionalNestedFunctionTypeWithOptionalArgument:(arg0:Int, ?((?arg0:String, ?Int) -> Void)) -> Void;
  }
}
package ;

class OldFunctionTypeSyntax {
  public static function main() {
    var simple:Int->Void;
    var simpleNoArguments:Void->Void;
    var simpleArgumentsList:Int->Bool->String->Void;

    var simpleOptionalArgument:?Int->Void;
    var simpleOptionalArgumentsList:Int->?String->?Bool->Void;

    var nestedFunctionType:Int->(Void->Void)->Void;
    var optionalNestedFunctionType:Int->?(Void->Void)->Void;

    var nestedFunctionTypeWithOptionalArgument:Int->(?String->Void)->Void;
    var optionalNestedFunctionTypeWithOptionalArgument:Int->?(?String->Void)->Void;

  }
}
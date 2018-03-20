class NewFunctionSyntax {
  public static function main() {
    // Old function type syntax
    var oldSyntax:Bool->?Int->Void;
    // New function type syntax
    var haxe4Syntax:(intValue:Int, ?String, ?optional:Bool) -> Void;

    // Mixed function types old in new syntax
    var oldInNewMixed:(closure:(Void -> Void), ?Bool) -> Void;
    // Mixed function types new in old syntax
    var newInOldMixed:Int->((a:Int, ?b:Bool)->Void)->Void;
  }
}
package ;

class ParenthesizedFunctionTypeSyntax {
  public function updateHitpoints(hp:Int, getCallback:Null<()->(()->Void)> = null):Bool {
    return true;
  }

  public function moveUnit(getWalkFinishedCallback:Null<(unit:String)->(()->Void)>):Bool {
    return false;
  }

  public function dispose() {
  }

  public static function main() {
    var newStyleParenReturn:()->(()->Void);
    var oldStyleParenReturn:Int->(()->Void);
    var parenthesizedFunctionType:(()->Void);
    var parenthesizedSimpleType:(Int);
  }
}

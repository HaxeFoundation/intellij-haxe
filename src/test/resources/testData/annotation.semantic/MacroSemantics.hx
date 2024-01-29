package ;
class MacroReifiction {


    macro public static function macroArrayReification(inputArray:Expr) {
        var myArray = macro $a{ inputArray };
        return myArray;
    }

    macro public static function macroDynamicReification(dynamicType:ExprOf<String>) {
        var myString = macro { $v{ dynamicType } }
        return myString ;
    }

    macro public static function macroBlockReification(a:Array<Expr>) {
        return macro $b{a};
    }

    macro public static function macroIdentifierReification(identifier:String) {
        return macro $i{identifier};
    }

    macro public static function macroFieldReification(field:Array<String>) {
        return macro ($p{field});
    }

    macro public static function macroExpReification(e:Expr):Dynamic {
        return macro (function() return $e);
    }

}
class MacroMethodCalls {


    public function test() {
        MacroMethodCalls.staticMethod("");
        var tmp:MacroMethodCalls;
        tmp.memberMethod(""); //Correct, only 1 arg expected
    }

    public macro function memberMethod(eThis:Expr, eValue:ExprOf<String>) {
        return null;
    }

    public macro static function staticMethod(eValue:ExprOf<String>) {
        return null;
    }
}
import haxe.macro.Expr;
using StringTools;
class ParameterInlayTest {

    public function new (valueA:String, ?valueB:Int, ?ValueC:Float) {}

    public function normalMember (valueA:String, ?valueB:Int, ?ValueC:Float) {}
    public static function staticMethod (valueA:String, ?valueB:Int, ?ValueC:Float) {}

    public macro function macroMamber (self:Expr, ?valueB:Int, ?ValueC:Float) {}
    public macro static function macroStatic (valueA:String, ?valueB:Int, ?ValueC:Float) {}

    static function tests() {
        var variable = new ParameterInlayTest("string", 1.0);

        normalMember("string", 1.0);
        staticMethod("string", 1.0);

        "".htmlEscape(true);
        StringTools.htmlEscape("", true);

        macroStatic("string", 1.0);
        variable.macroMamber(1, 1.0);
    }
}
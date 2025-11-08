import haxe.macro.Expr;
using StringTools;
class ParameterInlayTest {

    public function new (valueA:String, ?valueB:Int, ?ValueC:Float) {}

    public function normalMember (valueA:String, ?valueB:Int, ?ValueC:Float) {}
    public static function staticMethod (valueA:String, ?valueB:Int, ?ValueC:Float) {}

    public macro function macroMamber (self:Expr, ?valueB:Int, ?ValueC:Float) {}
    public macro static function macroStatic (valueA:String, ?valueB:Int, ?ValueC:Float) {}

    static function tests() {
        var variable = new ParameterInlayTest(/*<# valueA #>*/"string", /*<# ValueC #>*/1.0);

        normalMember(/*<# valueA #>*/"string", /*<# ValueC #>*/1.0);
        staticMethod(/*<# valueA #>*/"string", /*<# ValueC #>*/1.0);

        "".htmlEscape(/*<# quotes #>*/true);
        StringTools.htmlEscape(/*<# s #>*/"", /*<# quotes #>*/true);

        macroStatic(/*<# valueA #>*/"string", /*<# ValueC #>*/1.0);
        variable.macroMamber(/*<# valueB #>*/1, /*<# ValueC #>*/1.0);
    }
}
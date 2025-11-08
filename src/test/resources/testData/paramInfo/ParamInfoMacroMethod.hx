class ParameterInfoTest {
    public function new () {}
    public macro function macroMamber (self:Expr, valueB:Int = 1, ValueC:Float = 1.0) {}

    static function tests() {
        var variable = new ParameterInfoTest();
        variable.macroMamber(1,<caret>1.0);
    }
}
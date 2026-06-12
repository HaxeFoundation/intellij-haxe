package ;
// expected types verified against the Haxe compiler (4.2.5, 4.3.7 and 5.0.0-preview all agree)
class NumericInferenceHints {
    public function new(cond:Bool) {
        // int literal stays Int
        var fromIntLiteral/*<# :|Int #>*/ = 0;

        // division always yields Float in Haxe, even with two Int operands
        var fromDivision/*<# :|Float #>*/ = 1 / 2;

        // ternary branches unify Int and Float to Float
        var fromTernary/*<# :|Float #>*/ = cond ? 0 : 0.5;

        // arithmetic on int literals stays Int
        var fromIntArith/*<# :|Int #>*/ = 1 + 2;

        // mixed arithmetic promotes to Float
        var fromMixedArith/*<# :|Float #>*/ = 2 * 0.5;

        // division via variables, not literals
        var intA/*<# :|Int #>*/ = 10;
        var intB/*<# :|Int #>*/ = 4;
        var fromVarDivision/*<# :|Float #>*/ = intA / intB;

        // Std.int converts explicitly
        var fromStdInt/*<# :|Int #>*/ = Std.int(1.5);
    }
}

package testData.inlay.semantic;


/**
    Dynamic does not have implicit or direct cast, the compiler determines what operator expressions evaluates to.
    https://github.com/HaxeFoundation/haxe/blob/56b69a96d91011311d9965a1737d55b8789ff6f3/src/typing/operators.ml#L194
**/
class OperatorTest {
    public function test() {
        var d:Dynamic = null;

        var i:Int = 1;
        var f:Float = 1.0;
        var s:String = "hello";
        var b:Bool = true;

        // =====================================================
        // +
        // =====================================================

        var add_di/*<# :|Dynamic #>*/  = d + i; // Dynamic
        var add_id/*<# :|Dynamic #>*/ = i + d; // Dynamic

        var add_df/*<# :|Dynamic #>*/ = d + f; // Dynamic
        var add_fd/*<# :|Dynamic #>*/ = f + d; // Dynamic

        var add_ds/*<# :|String #>*/ = d + s; // String
        var add_sd/*<# :|String #>*/ = s + d; // String

        var add_dd/*<# :|Dynamic #>*/ = d + d; // Dynamic

        // =====================================================
        // -
        // =====================================================

        var sub_di/*<# :|Float #>*/ = d - i; // Float
        var sub_id/*<# :|Float #>*/ = i - d; // Float
        var sub_dd/*<# :|Float #>*/ = d - d; // Float

        // =====================================================
        // *
        // =====================================================

        var mul_di/*<# :|Float #>*/ = d * i; // Float
        var mul_id/*<# :|Float #>*/ = i * d; // Float
        var mul_dd/*<# :|Float #>*/ = d * d; // Float

        // =====================================================
        // /
        // =====================================================

        var div_di/*<# :|Float #>*/ = d / i; // Float
        var div_id/*<# :|Float #>*/ = i / d; // Float
        var div_dd/*<# :|Float #>*/ = d / d; // Float

        // =====================================================
        // %
        // =====================================================

        var mod_di/*<# :|Float #>*/ = d % i; // Float
        var mod_id/*<# :|Float #>*/ = i % d; // Float
        var mod_dd/*<# :|Float #>*/ = d % d; // Float

        // =====================================================
        // Bitwise
        // =====================================================

        var and_di/*<# :|Int #>*/ = d & i; // Int
        var and_id/*<# :|Int #>*/ = i & d; // Int
        var and_dd/*<# :|Int #>*/ = d & d; // Int

        var or_di/*<# :|Int #>*/ = d | i; // Int
        var or_id/*<# :|Int #>*/ = i | d; // Int
        var or_dd/*<# :|Int #>*/ = d | d; // Int

        var xor_di/*<# :|Int #>*/ = d ^ i; // Int
        var xor_id/*<# :|Int #>*/ = i ^ d; // Int
        var xor_dd/*<# :|Int #>*/ = d ^ d; // Int

        var shl_di/*<# :|Int #>*/ = d << i; // Int
        var shl_id/*<# :|Int #>*/ = i << d; // Int

        var shr_di/*<# :|Int #>*/ = d >> i; // Int
        var shr_id/*<# :|Int #>*/ = i >> d; // Int

        var ushr_di/*<# :|Int #>*/ = d >>> i; // Int
        var ushr_id/*<# :|Int #>*/ = i >>> d; // Int

        // =====================================================
        // Equality
        // =====================================================

        var eq_di/*<# :|Bool #>*/ = d == i; // Bool
        var eq_id/*<# :|Bool #>*/ = i == d; // Bool
        var eq_dd/*<# :|Bool #>*/ = d == d; // Bool

        var neq_di/*<# :|Bool #>*/ = d != i; // Bool
        var neq_id/*<# :|Bool #>*/ = i != d; // Bool
        var neq_dd/*<# :|Bool #>*/ = d != d; // Bool

        // =====================================================
        // Relational
        // =====================================================

        var lt_di/*<# :|Bool #>*/ = d < i; // Bool
        var lt_id/*<# :|Bool #>*/ = i < d; // Bool
        var lt_dd/*<# :|Bool #>*/ = d < d; // Bool

        var gt_di/*<# :|Bool #>*/ = d > i; // Bool
        var gte_di/*<# :|Bool #>*/ = d >= i; // Bool
        var lte_di/*<# :|Bool #>*/ = d <= i; // Bool

        // =====================================================
        // Boolean
        // =====================================================

        var and_db/*<# :|Bool #>*/ = d && b; // Bool
        var and_bd/*<# :|Bool #>*/ = b && d; // Bool
        var and_dd/*<# :|Bool #>*/ = d && d; // Bool

        var or_db/*<# :|Bool #>*/ = d || b; // Bool
        var or_bd/*<# :|Bool #>*/ = b || d; // Bool
        var or_dd/*<# :|Bool #>*/ = d || d; // Bool
    }
}
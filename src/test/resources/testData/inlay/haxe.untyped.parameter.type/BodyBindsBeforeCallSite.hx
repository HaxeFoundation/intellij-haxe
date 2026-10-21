class Test {

    public static function main() {
        scale(2);
    }

    // body usage binds Float; the Int at the call site unifies into it and
    // must NOT override the body-derived type
    static function scale(factor/*<# :|Float #>*/) {
        var f:Float = factor;
    }
}

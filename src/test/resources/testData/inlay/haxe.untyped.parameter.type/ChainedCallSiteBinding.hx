class Test {

    public static function main() {
        outer(7);
    }

    static function outer(value/*<# :|Int #>*/) {
        inner(value);
    }

    // inner's only call site passes outer's untyped parameter; the probe
    // chains through it to outer's own call site and finds the Int there
    static function inner(nested/*<# :|Int #>*/) {
    }
}

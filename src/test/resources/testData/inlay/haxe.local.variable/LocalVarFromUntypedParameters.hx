class Test {

    public static function main() {
        span(3, 9);
    }

    // the local's init depends on untyped parameters whose types only exist
    // at the call site; the probe chain must reach them (ArraySort's
    // "var len = to - from" pattern)
    static function span(from, to) {
        var len/*<# :|Int #>*/ = to - from;
    }
}

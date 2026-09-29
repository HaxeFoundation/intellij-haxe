class Test {

    public static function main() {
        // T binds to Int from the items argument; the lambda's untyped
        // parameter gets its type from the resolved fn parameter (T->T)
        apply([1, 2], n/*<# :|Int #>*/ -> n);
    }

    static function apply<T>(items:Array<T>, fn:T->T):Void {
    }
}

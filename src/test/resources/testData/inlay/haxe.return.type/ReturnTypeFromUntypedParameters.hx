class Test {

    public static function main()/*<# :|Void #>*/ {
        pick(3, 4);
    }

    // the return expression is an untyped parameter typed only via its call
    // site; the inferred return type must follow (ArraySort's gcd pattern)
    static function pick(m, n)/*<# :|Int #>*/ {
        return m;
    }
}

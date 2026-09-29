class Test {

    public static function main() {
        record("label", 42);
    }

    // the body never constrains the parameters, so the compiler binds them
    // at the first typed call site (stage 4 of monomorph binding)
    static function record(name/*<# :|String #>*/, count/*<# :|Int #>*/) {
    }
}

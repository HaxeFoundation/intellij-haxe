class Main {
    macro static function generateClass(funcName:String) {
        var c/*<# :|TypeDefinition #>*/ = macro class MyClass {
            public function new() {}

            public function $funcName() {
                trace($v{funcName} + " was called");
            }
        }
        haxe.macro.Context.defineType(c);

        var x/*<# :|ExprOf|<|MyClass|> #>*/ = macro new MyClass();
        return x;
    }

    public static function main() {
        // tests that we "unwrap" expression when leaving macro scope
        var c/*<# :|MyClass #>*/ = generateClass("myFunc");
    }
}
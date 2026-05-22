package;

typedef Spec = { name:String };

class Holder {
  public function new(spec:Spec) {}
}

class Test {
  public static function main() {
    // The member 'name' has no value expression — GrammarKit error-recovery
    // produces a HaxeObjectLiteralElement whose getExpression() is null.
    // The line-marker / resolve pass must not crash with IllegalArgumentException
    // on the @NotNull 'element' parameter of HaxeExpressionEvaluator.evaluate.
    new Holder({name: });
  }
}

class GenericToGenericReference<T:String> {
  public static function test<S:T>() {
    var x:S;
    x.<caret>
  }
}
class GenericToGenericReference<T:String, S:T> {
  public static function test() {
    var x:S;
    x.<caret>
  }
}
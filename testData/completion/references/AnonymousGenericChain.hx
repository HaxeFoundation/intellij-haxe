class AnonymousGenericChain {
  public static function test() {
    var data:Structure<String>;
    data.deeply.nested.structure.value.<caret>
  }
}

typedef Structure<T> = {
  deeply:{
    nested: {
      structure: {
        value:T
      }
    }
  }
};

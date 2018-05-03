typedef ExtendedString<T> = {key:T};
class TypeDef2 {
  public function foo(){
    var test:ExtendedString<String>;
    test.key.leng<caret>th;
  }
}

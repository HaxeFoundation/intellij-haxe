typedef ExtendedString<T> = {>String, key:T}
class TypeDef2 {
  public function foo(){
    var test = new ExtendedString<String>();
    test.key.leng<caret>th;
  }
}
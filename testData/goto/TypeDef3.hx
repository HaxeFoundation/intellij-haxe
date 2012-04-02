typedef ExtendedString<T> = {>String, function key():T;}
class TypeDef3 {
  public function foo(){
    var test = new ExtendedString<String>();
    test.key().leng<caret>th;
  }
}
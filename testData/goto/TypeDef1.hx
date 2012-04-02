typedef ExtendedString = {>String, key:String}
class TypeDef1 {
  public function foo(){
    var test = new ExtendedString();
    test.leng<caret>th;
  }
}
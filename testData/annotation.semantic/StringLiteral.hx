package ;
class Test {
  public static function main() {
    var value:Int = 10;
    var s:String = <warning descr="Expressions with string interpolation should be wrapped by single quotes">"$value"</warning>;
    var obj = {"$value": "text"};
  }
}
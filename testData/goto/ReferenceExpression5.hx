import com.bar.Foo;
class ReferenceExpression5 {
  public static function main(){
    var foo:FooExtern = new FooExtern();
    foo.foo<caret>Extern;
  }
}
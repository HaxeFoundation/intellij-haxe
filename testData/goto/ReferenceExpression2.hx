import com.bar.Foo;

class ReferenceExpression2 {
  public static function main(){
    var foo:Foo = new Foo();
    foo.baz.ba<caret>r;
  }
}
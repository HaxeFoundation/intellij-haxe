import com.bar.Foo;

class VarDeclaration{
    public var fo<caret>o:Foo;
    public static function main(){
        var foo:Foo;
        foo = Foo();
        foo.bar();
    }
}
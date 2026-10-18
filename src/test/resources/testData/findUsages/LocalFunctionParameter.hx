import com.bar.Foo;

class FunctionParameter{
    public static function main(){
        var foo:Foo;
        function print(f<caret>oo:Foo){
            trace(foo.toString());
        }
        print(foo);
    }
}
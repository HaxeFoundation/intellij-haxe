import com.bar.Foo;

class FunctionParametr{
    public static function main(){
        var foo:Foo;
        function print(foo:F<caret>oo){
            trace(foo.toString());
        }
        print(foo);
    }
}
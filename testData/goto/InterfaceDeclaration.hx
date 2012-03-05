import com.bar.IBar;

class InterfaceDeclaration{
    public static function main(){
        var foo:IBar;
        function print(foo:IB<caret>ar){
            trace(foo.toString());
        }
        print(foo);
    }
}
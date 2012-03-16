import com.bar.ClassToFind;

class ClassDeclaration extends ClassToFind {
    public var foo:ClassToFind;
    public static function main(){
        var foo:ClassToFind;
        foo = ClassToFind();
        var temp = function(param:ClassToFind){
          trace(239);
        }
    }

    function print(param:ClassToFind){

    }
}
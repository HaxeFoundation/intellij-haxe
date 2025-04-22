import haxe.ds.StringMap;
import haxe.ds.Either;
import haxe.ds.Vector;
import haxe.ds.IntMap;
import haxe.ds.ObjectMap;
class OtherClassForGeneration {
        // including a type here to make sure we do not duplicate imports when geenerating method
        var stringMap:StringMap<Int>;

    public function testMethodInDifferentClass(a:StringMap<Int>, b:ObjectMap<{x:Either<List<String>, Vector<Int> -> IntMap<String>>}, Int>):Void {

    }
}

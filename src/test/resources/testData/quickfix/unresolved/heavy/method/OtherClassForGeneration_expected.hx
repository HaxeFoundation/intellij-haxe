import haxe.ds.StringMap;
import haxe.ds.ObjectMap;
class OtherClassForGeneration {
        // including a type here to make sure we do not duplicate imports when geenerating method
        var stringMap:StringMap<Int>;

    public function testMethodInDifferentClass(a:StringMap<Int>, b:ObjectMap<{}, Int>):Void {

    }
}

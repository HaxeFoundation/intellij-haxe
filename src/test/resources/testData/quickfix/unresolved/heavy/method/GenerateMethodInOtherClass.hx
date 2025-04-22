import haxe.ds.StringMap;
import haxe.ds.ObjectMap;
import haxe.ds.Either;
import haxe.ds.Vector;
import haxe.ds.IntMap;

class GenerateMethodInOtherClass {
    function test() {
        // verify both parameter generation and adding any missing imports in target.
        var paramA:StringMap<Int>;
        // verifying that we collect types for import checks also from anonymousType, Function and any spesifics
        var paramB:ObjectMap<{x:Either<List<String>,Vector<Int>->IntMap<String>>},Int>;

        var ref:OtherClassForGeneration = null;
        ref.testMethodInDifferentClass<caret>(paramA,paramB);
    }
}

import haxe.ds.StringMap;
import haxe.ds.ObjectMap;

class GenerateMethodInOtherClass {
    function test() {
        // verify both parameter generation and adding any missing imports in target.
        var paramA:StringMap<Int>;
        var paramB:ObjectMap<{},Int>;

        var ref:OtherClassForGeneration = null;
        ref.testMethodInDifferentClass<caret>(paramA,paramB);
    }
}

package;
class Test {
    public function new() {
        var arr1:Array<Int> = [for (i in 0...10) i];
        var arr2:Array<String> = [for (i in 0...10) "str" + i];

        var i = 0;
        var arr3:Array<Int> = [while (i < 10) i++];
        var arr4:Array<String> = [while (i < 10) "str" + i++];

        var arr5 = [while (i < 10) "str" + i++];
        arr5[0].substr(1,2); // verfiy generics


        // wrong: initilizer  returns string array
        var arr4:Array<Float> = <error descr="Incompatible type: Array<String> should be Array<Float>">[while (i < 10) "str" + i++]</error>;
    }
}
class DowncastTest {

    function test() {
        var dynamicValue:Dynamic;

        //CORRECT
        // make sure assign works ( had problem with missing typeParam. ~ Array should be Array<T>)
        var newVar:Array<String> = Std.downcast(dynamicValue, Array);

        // WRONG
        var newVar:String = <error descr="Incompatible type: Array<T> should be String">Std.downcast(dynamicValue, Array)</error>;
        var newVar:Map<String, String> = <error descr="Incompatible type: Array<T> should be Map<String, String>">Std.downcast(dynamicValue, Array)</error>;
    }
}
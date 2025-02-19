class DowncastTest {

    function test() {
        var dynamicValue:Dynamic;

        //CORRECT
        // make sure assign works ( had problem with missing typeParam. ~ Array should be Array<T>)
        var newVar:Array<String> = Std.downcast(dynamicValue, Array);

        // WRONG
        var <error descr="Incompatible type: Array<T> should be String">newVar:String = Std.downcast(dynamicValue, Array)</error>;
        var <error descr="Incompatible type: Array<T> should be Map<String, String>">newVar:Map<String, String> = Std.downcast(dynamicValue, Array)</error>;
    }
}
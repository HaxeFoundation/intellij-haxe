package testData.inlay.haxe.local.variable;

typedef ObjectDefinition<T> = {
    array:Array<T>
}

class ComplexTypeParameterMonomorphTest {

    public function testMethodCall() {

        // this variable should get ":Array<String>" from BuildContext parameter type
        // issues with recursion guard and incorrect caching will incorrectly cause this to become ":Array<Int>"
        var testArray/*<# :|Array|<|String|> #>*/ = [];

        morphByParameter({array: testArray });

        // its important that the monomorph of "testArray" does not try to pick up its type from this usage
        // it will then default to Array<Int> as  the parameters are optional and it we would incorreclty
        // use the first one

        useMorphedValue(testArray);
    }
    public function testFunctionType(morphByFunctionCall:ObjectDefinition<Float>->Void) {

        // this variable should get ":Array<Float>" from the parameter type BuildContext as part as the fuctionType
        // issues with recursion guard and incorrect caching will incorrectly cause this to become ":Array<Int>"
        var testArray/*<# :|Array|<|Float|> #>*/ = [];

        morphByFunctionCall({array: testArray });

        // its important that the monomorph of "testArray" does not try to pick up its type from this usage
        // it will then default to Array<Int> as  the parameters are optional and it we would incorreclty
        // use the first one

        useMorphedValue(testArray);
    }

    public function fromOptional() {

        var testArray/*<# :|Array|<|Int|> #>*/ = [];
        useMorphedValue(testArray);
    }
    public static function morphByParameter(value:ObjectDefinition<String>) {}
    public static function useMorphedValue(?optional1:Array<Int>, ?optional2:Array<String>, ?optional3:Array<Float>) {}
}
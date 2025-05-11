package ;
class FunctionTypeAssignTest3 {

    static function testOptionalArgs() {
        // Correct
        var testOptional: String->Int->Void = optionalArg2;
        var testDefault: String->Int->Void = defaultArg2;
        var testCombo: String->Int->Void = optionalArgDefault2;
        //
        var testOptional: String->?Int->Void = optionalArg2;
        var testDefault: String->?Int->Void = defaultArg2;
        var testCombo: String->?Int->Void = optionalArgDefault2;
        //
        var testDefaultEnum: String->?TestEnum->Void = defaultEnumArg;

        // wrong
        var optionalMismatch: String->?Int->Void = <error descr="Incompatible type: (String, Int)->Void should be (String, ?Int)->Void">noOptionalArgs</error>;

        // wrong
        var ignoreArgInDef: String->Void = <error descr="Incompatible type: (String, ?Int)->Void should be String->Void">optionalArg2</error>;
        var ignoreArgInDef: String->Void = <error descr="Incompatible type: (String, ?Int)->Void should be String->Void">defaultArg2</error>;
        var ignoreArgInDef: String->Void = <error descr="Incompatible type: (String, ?Int)->Void should be String->Void">optionalArgDefault2</error>;

    }

    static function optionalArg2(arg1:String, ?arg2:Int) {

    }

    static function defaultArg2(arg1:String, arg2:Int = 1) {

    }

    static function optionalArgDefault2(arg1:String, ?arg2 = 1) {

    }
    static function noOptionalArgs(arg1:String, arg2:Int) {

    }

    static function defaultEnumArg(arg1:String, arg2 = Value1) {

    }

}
enum TestEnum {
    Value1;
    Value2;
}
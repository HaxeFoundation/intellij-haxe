package testData.inlay.haxe.local.variable;
typedef SomeTypeDef = {
    var normalA:Int;
    final normalB:String;
    @:optional var optionalC:String;
}
typedef SomeStruct = {
    ?optinal:Int,
}
class NullWrapping {
    public function optionalStruct(s:SomeStruct) {
        var x/*<# :|Null|<|Int|> #>*/ = s.optinal;
    }
    public function optionalTypeDef(s:SomeTypeDef) {
        var a/*<# :|Int #>*/ = s.normalA;
        var b/*<# :|String #>*/ = s.normalB;
        var c/*<# :|Null|<|String|> #>*/ = s.optionalC;
    }

    public function optionalTypeA(?i:String) {
        var x/*<# :|Null|<|String|> #>*/ = i;
    }

    public function optionalTypeB(?i = "Str") {
        var x/*<# :|Null|<|String|> #>*/ = i;
    }

    public function optionalTypeParameter<T>(?i:T) {
        var x/*<# :|Null|<|T|> #>*/ = i;
    }

    public function defaultType(i = "String") {
        var x/*<# :|String #>*/ = i;
    }
}
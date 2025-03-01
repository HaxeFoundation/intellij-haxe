/*
*  Testing that untyped variables get unified correctly when init expression is null (Expectes Null<T> types)
*/
class PrimitiveNullUnification {

    var nullTypedMember:Null<String> = "" ;

    public function new() {
        nullOfUnknownTest();
        nullOfIntTest();
        nullOfStringTest();
        nullOfStringFromNullTypedMember();
        nullOfEmptyArrayTest();
        nullOfStringArrayTest();
    }

    function nullOfUnknownTest() {
        //  Null<Unknown<0>>;
        var noType/*<# :|Dynamic #>*/ = null;
        return noType;
    }

    function nullOfStringFromNullTypedMember() {
        //Null<String>
        var nullOfStringFromMember/*<# :|Null|<|String|> #>*/ = null;
        nullOfStringFromMember = nullTypedMember;

        var nullOfStringTest/*<# :|String #>*/ = nullOfStringFromMember.charAt(0);
        return nullOfStringTest;
    }

    function nullOfIntTest() {
        //Null<Int>
        var nullOfInt/*<# :|Null|<|Int|> #>*/ = null;
        nullOfInt = 1;

        var nullOfIntTest/*<# :|Int #>*/ = 1 + nullOfInt;
        return nullOfIntTest;
    }

    function nullOfStringTest() {
        //Null<String>
        var nullOfString/*<# :|Null|<|String|> #>*/ = null;
        nullOfString = "";

        var nullOfStringTest/*<# :|String #>*/ = nullOfString.charAt(0);
        return nullOfStringTest;
    }

    function nullOfEmptyArrayTest() {
        //Null<Array<Unknown<0>>>
        var nullOfEmptyArray/*<# :|Null|<|Array|<|unknown|>|> #>*/ = null;
        nullOfEmptyArray = [];

        var nullOfEmptyArrayTest/*<# :|Int #>*/ = nullOfEmptyArray.indexOf("");
        return nullOfEmptyArrayTest;
    }

    function nullOfStringArrayTest() {
        //Null<Array<String>>
        var nullOfEmptyArray/*<# :|Null|<|Array|<|String|>|> #>*/ = null;
        nullOfEmptyArray = ["str"];

        var nullOfEmptyArrayTest/*<# :|Int #>*/ = nullOfEmptyArray.indexOf("");
        return nullOfEmptyArrayTest;
    }
}
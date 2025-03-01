class Untypedgenerics<T> {

    public static function main() {
        var myArr =  ["test"];
        // function literal
        myArr.filter(function(arg/*<# :|String #>*/) {return true;});
        // lambda
        myArr.filter((arg/*<# :|String #>*/) -> arg.length > 0);
        myArr.filter(arg/*<# :|String #>*/ ->  arg.length < 0);
        myArr.sort((s1/*<# :|String #>*/,s2/*<# :|String #>*/)-> -1);

        testMethodGenericPassing("", (arg/*<# :|String #>*/)-> {});
        testMethodGenericPassing(1, (arg/*<# :|Int #>*/)-> {});

        var instance = new Untypedgenerics<Float>();
        instance.testClassGenericPassing(1.0, (arg/*<# :|Float #>*/)->{});
    }

    function testMethodGenericPassing <T>(first:T, method:T->Void){}
    function testClassGenericPassing (first:T, method:T->Void){}
}
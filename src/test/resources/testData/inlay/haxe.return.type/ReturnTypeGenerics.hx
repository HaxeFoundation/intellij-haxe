class ReturnTypeGenerics<T> {
    public function new() {
        function localFnVoid()/*<# :Void #>*/ {}
        function localFnStr()/*<# :String #>*/ {return "";}
        function localFnArg(a:Int)/*<# :Int #>*/ {return a;}
        function localFnClassGeneric(a:T)/*<# :T #>*/ {return a;}

        // TODO need fix: fails if anonymousFn is below localFnCall
        var anonymousFn = function (){1;};
        function localFnCall()/*<# :Int #>*/ {return anonymousFn();}

        var anonymousFnArg = function (z:String){z;};
        function localFnArgCall()/*<# :String #>*/ {return anonymousFnArg("");}


    }
    public static function staticfn(hello:String)/*<# :String #>*/ {
        return  hello + " world";
    }
    public static function staticGenericFn<T>(x:T)/*<# :T #>*/ {
        return x;
    }

    public static function staticGenericFn2<T:ReturnTypeGenerics<String>>(x:T)/*<# :T:ReturnTypeGenerics<String> #>*/ {
        return x;
    }
}

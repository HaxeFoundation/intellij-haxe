class DynamicMethodFunctionAssignInlay<C:{length:Int}> {

    var fntype:Int->Int;

    public function new() {
        var x = new DynamicMethodFunctionAssignInlay();
        fntype = (x/*<# :|Int #>*/)  -> { x * 2; };
        x.dynamicFn = (x/*<# :|String #>*/)  -> { x.toLowerCase(); };
        x.dynamicFn = function (x/*<# :|String #>*/)  { "myString"; };
        x.dynamicGenericFn = function (x/*<# :|C #>*/) { "myString"; };
    }

    public dynamic function dynamicFn(x:String):String {return "";}
    public dynamic function dynamicGenericFn(x:C):C {return x;}
}
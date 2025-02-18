package ;
class AssignDynamicMethod<C:{var length(default, null):Int; }> {

    public function new() {
        var x = new AssignDynamicMethod();
        // correct
        x.dynamicFn = () -> { "myString"; };
        x.dynamicFn = () -> { getType(String); };

        // wrong
        x.dynamicFn = <error descr="Incompatible type: Void->Int should be Void->String">() -> { 1; }</error>;
        x.dynamicFn = <error descr="Incompatible type: Void->Int should be Void->String">() -> { getType(Int); }</error>;

        //correct
        x.dynamicGenericFn = (x) -> { "myString"; };
        x.dynamicGenericFn = (x) -> { getType(String); };

        x.dynamicGenericFn = function (x) { "myString"; };
        x.dynamicGenericFn = function (x) { getType(String); };

        //wrong
        x.dynamicGenericFn = <error descr="Incompatible type: C:{var length(default, null):Int; }->Int should be C:{var length(default, null):Int; }->C:{var length(default, null):Int; }">(x) -> { 1; }</error>;
        x.dynamicGenericFn = <error descr="Incompatible type: C:{var length(default, null):Int; }->Int should be C:{var length(default, null):Int; }->C:{var length(default, null):Int; }">(x) -> { getType(Int); }</error>;


        //wrong : Cannot rebind non dynamic
        <error descr="Cannot rebind this method : please use 'dynamic' before method declaration">x.staticFn = () -> { getType(String); }</error>;
        <error descr="Cannot rebind this method : please use 'dynamic' before method declaration">x.staticFn = () -> { getType(Int); }</error>;

    }

    static function getType<T>(type:Class<T>):T {
        return null;
    }

    public dynamic function dynamicFn():String {return "";}

    public dynamic function dynamicGenericFn(x:C):C {return x;}

    public function staticFn():String { return "";}
}

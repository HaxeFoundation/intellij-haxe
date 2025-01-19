package ;
class AssignDynamicMethod<C:{length:Int}> {

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

        //wrong
        x.dynamicGenericFn = <error descr="Incompatible type: unknown->Int should be C:{length:Int}->C:{length:Int}">(x) -> { 1; }</error>;
        x.dynamicGenericFn = <error descr="Incompatible type: unknown->Int should be C:{length:Int}->C:{length:Int}">(x) -> { getType(Int); }</error>;


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

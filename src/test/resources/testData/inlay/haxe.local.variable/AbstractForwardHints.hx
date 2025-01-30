typedef Wrapper<T> = T;

class Data<T> {
    public var x:Float;
    public var y:T;
}

@:forward
abstract WrapperTest(Wrapper<Data<String>>) {}
@:forward
abstract WrapperGenericTest<T>(Wrapper<T>) {}
@:forward
abstract NoWrapperGeneric<T>(T) {}

@:forward
abstract ArrayAccessGenericWrapper<T>(Array<Wrapper<T>>) {
    @:arrayAccess public inline function get(k:Int):Null<T>{
        var item/*<# :Wrapper<T> #>*/ = this[0];
        return item;
    }
}

class ForwardTest {
    public function new() {
        var noWrapGeneric:NoWrapperGeneric<String>;
        var testA/*<# :String #>*/ = noWrapGeneric.toLowerCase();

        var wrapper:WrapperTest;
        var testTyped/*<# :Float #>*/ = wrapper.x;
        var testGeneric/*<# :String #>*/  = wrapper.y;

        var wrapperGeneric:WrapperGenericTest<Array<String>>;
        var firstGeneric/*<# :Array<String> #>*/ = wrapperGeneric.splice(0,1);
        var pop/*<# :Null<String> #>*/ = wrapperGeneric.pop();

        //TODO this should not be allowed
        var wrong = wrapperGeneric[0]; // WRONG no array access declared for abstract

        var arr:ArrayAccessGenericWrapper<String>;
        var get/*<# :Null<String> #>*/ = arr.get(0);
        var arrs/*<# :Null<String> #>*/ = arr[0];
    }
}
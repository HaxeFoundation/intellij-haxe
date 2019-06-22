// From Haxe compiler unit tests: unit/TestSpecification.hx
// This should NOT produce an error at the overridden Int value.
package;

@:rtti
@:keepSub
class RttiClass1 {
    static var v:String;
    public function f() {
        return 33.0;
    }
}

class RttiClass3 extends RttiClass1 {
    override public function f():Int {
        return 33;
    }
}
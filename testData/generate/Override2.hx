class A extends B {
    <caret>
}

class B extends C {
    override public function a():Void {
    }

    public function d() {
    }
}

class C {
    public function new() { }

    public function a():Void {

    }
    @:final public function b():Void {

    }
    static public function c():Void {

    }
}
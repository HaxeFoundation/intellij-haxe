class AinBOverloadTest {
    public function new() {
        var myInt:MyInt = 1;
        // valid
        var x = myInt in [1, 2, 3];
        // invalid
        var x = <error descr="Unable to apply operator in for types MyInt and String = 'String'">myInt in "String"</error>;
    }
}

abstract MyInt(Int) from Int to Int {

    public function new(value:Int) {
        this = value;
    }

    @:op(A in B)
    public static inline function inOp(self:MyInt, collection:Array<Int>):Bool {
        return collection.contains(self);
    }
}
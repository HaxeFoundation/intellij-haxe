package ;

@:forward
abstract AbstractWrapper<T>(T) from T to T {
    public inline function new(value:T) this = value;
    public function extraFunctionality() : T {return this;}
}

class TestIssue772 {
    public function new() {
        var obj:AbstractWrapper<String> = new String();
        obj.<caret>      // no code tips here
    }
}

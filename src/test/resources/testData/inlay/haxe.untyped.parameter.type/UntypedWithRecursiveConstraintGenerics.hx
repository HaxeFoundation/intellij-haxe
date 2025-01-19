class Untypedgenerics<T> {
    function sort(list:TestClass) {
        //sort<T:{prev:T, next:T}>(list:T, cmp:T->T->Int):T
        haxe.ds.ListSort.sort(list, function(p1/*<# :TestClass #>*/, p2/*<# :TestClass #>*/) return p1.value < p2.value ? 1 : -1);
        haxe.ds.ListSort.sort(list, (p1/*<# :TestClass #>*/, p2/*<# :TestClass #>*/) -> p1.value < p2.value ? 1 : -1);
    }
}

class TestClass {
    public var next:TestClass;
    public var prev:TestClass;
    public var value:Int;
}
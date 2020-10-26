package ;
class ToFromAnnotations {
    public function new() {
    }
    public function test() {

        // OK; got @:from Metadata on method to convert
        var assignFrom:MyAbstract = "3" ;
        // OK; got @:to Metadata on method to convert
        var assignTo:Array<Int> = assignFrom ;

        // WRONG: while an abstract of int it can not be assigned to an int
        var <error descr="Incompatible type: Int should be MyAbstract">wrongUse1:MyAbstract = 1</error>;
        // WRONG:  while an abstract of int its not a int value
        var <error descr="Incompatible type: MyAbstract should be Int">wrongUse2:Int = assignFrom</error>;

        //  OK: Any has @:to method with Type Parameter T, all types are accepted
        var anyVar:Any = assignFrom;
        // OK: Any has @:from method with Type Parameter T, all types are accepted
        var myAbstractVar:MyAbstract =  anyVar;
    }
}


abstract MyAbstract(Int) {
    inline function new(i:Int) {
        this = i;
    }

    @:from
    static public function fromString(s:String) {
        return new MyAbstract(Std.parseInt(s));
    }

    @:to
    public function toArray() {
        return [this];
    }
}
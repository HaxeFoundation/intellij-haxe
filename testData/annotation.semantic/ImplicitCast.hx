package ;
typedef MyAbstractAsTypeDef = MyAbstract;
class ImplicitCast {
    public function new() {
    }
    public function test() {
        var assignFrom:MyAbstract = "3" ; // OK; got @:from Metadata on method to convert
        var assignTo:Array<Int> = assignFrom ;  // OK; got @:to Metadata on method to convert

        var anyVar:Any = assignFrom ; //  OK: Any extends dynamic and has a @:to  with Type Parameter T
        var myAbstractVar:MyAbstract =  anyVar; // OK: Any extends dynamic and has a @:from with Type Parameter T

        var typeDefVar:MyAbstractAsTypeDef = "2"; //OK,  typeDef resolves to abstract with implicit cast

        var <error descr="Incompatible type: Int should be MyAbstract">wrongUse1:MyAbstract = 1</error>;  // WRONG: while an abstract of int it is not the same as an int.
        var <error descr="Incompatible type: MyAbstract should be Int">wrongUse2:Int = assignFrom</error>; //WRONG  while this is an abstract of in we do not have a converter method.
        var <error descr="Incompatible type: Int should be MyAbstractAsTypeDef">wrongUse3:MyAbstractAsTypeDef = 2</error>; //WRONG, typeDef resolves to abstract that does not have a @:from method for int

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



package ;
typedef MyAbstractAsTypeDef = MyAbstract;
class ImplicitCast {

    public function test() {

        // Normal

        var assignFrom:MyAbstract = "3" ; // OK; got @:from Metadata on method to convert
        var assignTo:Array<Int> = assignFrom ;  // OK; got @:to Metadata on method to convert

        var anyVar:Any = assignFrom ; //  OK: Any extends dynamic and has a @:to  with Type Parameter T
        var myAbstractVar:MyAbstract =  anyVar; // OK: Any extends dynamic and has a @:from with Type Parameter T

        var typeDefVar:MyAbstractAsTypeDef = "2"; //OK,  typeDef resolves to abstract with implicit cast

        var wrongUse1:MyAbstract = <error descr="Incompatible type: Int should be MyAbstract">1</error>;  // WRONG: while an abstract of int it is not the same as an int.
        var wrongUse2:Int = <error descr="Incompatible type: MyAbstract should be Int">assignFrom</error>; //WRONG  while this is an abstract of in we do not have a converter method.
        var wrongUse3:MyAbstractAsTypeDef = <error descr="Incompatible type: Int should be MyAbstractAsTypeDef">2</error>; //WRONG, typeDef resolves to abstract that does not have a @:from method for int

        // Class Generics

        var genericFromString:MyClassGenericAbstract<String> = "implicit cast from string";
        var genericFromInt:MyClassGenericAbstract<Int> = 1;

        var genericToString:String = genericFromString;
        var genericToInt:Int = genericFromInt;

        var wrongTypeFrom:MyClassGenericAbstract<String> =  <error descr="Incompatible type: Int should be MyClassGenericAbstract<String>">1</error>;
        var wrongTypeTo:Int = <error descr="Incompatible type: MyClassGenericAbstract<String> should be Int">wrongTypeFrom</error>;

        // Method Generics

        var genericFrom:MyMethodGenericAbstract<String> = "implicit cast from string";
        var genericTo:String = genericFrom;

        var wrongTypeFrom:MyMethodGenericAbstract<String> =  <error descr="Incompatible type: Int should be MyMethodGenericAbstract<String>">2</error>;
        var wrongTypeTo:Int = <error descr="Incompatible type: MyMethodGenericAbstract<String> should be Int">wrongTypeFrom</error>;

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

abstract MyClassGenericAbstract<T>(Array<T>) {
    inline function new(arr:Array<T>) {
        this = arr;
    }

    @:from
    static public function fromGeneric(value:T) {
        return new MyClassGenericAbstract([value]);
    }

    @:to
    public function toArray() {
        return this.pop();
    }
}

abstract MyMethodGenericAbstract<Q>(Array<Q>) {
    inline function new(arr:Array<Q>) {
        this = arr;
    }

    @:from
    static public function fromGeneric<T>(value:T) {
        return new MyMethodGenericAbstract([value]);
    }

    @:to
    public function toArray() {
        return this.pop();
    }
}



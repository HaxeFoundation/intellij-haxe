class Level1<T:{length:Int}> {
    var value1:T;
    function  getV1()return value1;

    public function new () {
        var x = value1.length * 2;
    }
}
// with defaults inherits constriants
class Level2<Q = String> extends Level1<Q> {
    var value2:Q;
    function  getV2()return value2;

    public function new () {
        var v1 = getV1().toLowerCase();
        var v2 = getV2().charAt(0);
        var quad = v1.length * v2.length;
        value2 = v1;
    }
}

// override defaults
class Level3A<U:Array<String>> extends Level2<U> {
    var value3:U;
    function  getV3()return value2;

    public function new () {
        var v1:Null<String> = getV1().pop();
        var v2 = getV2().iterator();
        var v3 = getV3().length;

        var z = getV1();
        var str = z.pop().toString();
    }
}
// inherit default
class Level3B extends Level2 {

    var v1:String = getV1();
    var v2 = getV2();

    public function new () {
        var za = getV1();
        var str = za.toLowerCase();

        // verify that unresolved symbol check is enabled
        var notFound = this.<warning descr="Unresolved symbol">doesNotExsist</warning>;
    }
}
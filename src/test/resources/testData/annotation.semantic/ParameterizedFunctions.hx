package ;
class Test {
    static function main()
    {
        var value:String = "asd";

        var c:Clazz = new Clazz();
        var str:String = c.getSome(value);
        var str:Int = c.getSome(1);
        var str:String = <error descr="Incompatible type: Int should be String">c.getSome(1)</error>; // Should be an error: Incompatible type

        var fun:String->Int = c.getSome(function(myVal:String) {return 1;});
        var fun:String->Int = c.getSome(function(myVal) {return 1;});
        var fun:String->Int = <error descr="Incompatible type: Float->Int should be String->Int">c.getSome(function(myVal:Float) { return 1;})</error>;  // Should be an error: Incompatible type
    }
}

class Clazz
{
    public function new() { }

    public function getSome<T>(value:T):T
    {
        return value;
    }
}
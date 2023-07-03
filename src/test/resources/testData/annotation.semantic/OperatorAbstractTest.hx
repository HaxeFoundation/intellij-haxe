package ;

class OperatorAbstractTest {
    var a:IntToFromKeywords;
    var b:IntToFromMeta;
    var c:PlainAbstract;
    var d:AbstractWithOverloads;

    public function new() {


        <error descr="Unable to apply operator + for types IntToFromKeywords and IntToFromKeywords">a + a</error>; // WRONG no operator defined
        <error descr="Unable to apply operator + for types IntToFromMeta and IntToFromMeta">b + b</error>; // WRONG no operator defined
        <error descr="Unable to apply operator + for types PlainAbstract and PlainAbstract">c + c</error>; // WRONG no operator defined

        a + 1; // CORRECT (a can be casted TO int, so  int + int)
        d + d; // CORRECT operator defined

        <error descr="Unable to apply operator - for types AbstractWithOverloads and AbstractWithOverloads">d - d</error>; // WRONG operator defined

        // Wrong (not sure why @:to/from is diffrent from the keyword ones, but this one will not work)
        <error descr="Unable to apply operator + for types IntToFromMeta and Int = 1">b + 1</error>;
    }

    public function testOperatorOverloads() {
        d++; // CORRECT operator defined
    }
}

abstract PlainAbstract (Float) {}
abstract IntToFromKeywords (Int) to Int from Int {}
abstract IntToFromMeta(String) {
    inline function new(s:String) {
        this = s;
    }

    @:from
    static public function fromInt(s:Int):IntToFromMeta {
        return new IntToFromMeta("" + s);
    }

    @:to
    public function toInt():Int {
        return 1;
    }
}


abstract AbstractWithOverloads (Float) {
    @:op(A+B)
    public static function addInts(a:AbstractWithOverloads, b :AbstractWithOverloads):Int {return 1;}
    @:op(A++)
    public static function inc(a:AbstractWithOverloads):Int {return 1;}
}
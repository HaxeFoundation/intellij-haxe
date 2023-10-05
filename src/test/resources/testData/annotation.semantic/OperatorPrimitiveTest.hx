package ;
// more info at https://haxe.org/manual/expression-operators-binops.html
class OperatorTest {

    // ints
    var i = 1;
    var j = 2;
    // floats
    var f = 0.5;
    var k = 1.5;
    // bools
    var a = true;
    var b = false;
    //strings
    var s = "ABC";
    var t = "ZYX";

    var toDyn:Dynamic;
    var toInt:Int;
    var toFloat:Float;
    var toBool:Bool;
    var toString:String;

    public function new() {}

    public function TestInt(parameterA:Int, ParameterB = 0) {
        toInt = parameterA + ParameterB;
        toBool = <error descr="Unable to apply operator && for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">parameterA && ParameterB</error>;

        toInt = i + j;
        toInt = i - j;
        toInt = i * j;
        toInt = i % j;

        toInt = i ^  j;
        toInt = i |  j;
        toInt = i &  j;
        toInt = i << j;
        toInt = i >> j;
        toInt = i >>> j;

        toFloat = i / j;

        toBool = i == j;
        toBool = i != j;
        toBool = i <  j;
        toBool = i <= j;
        toBool = i >  j;
        toBool = i >= j;

        toBool = (i is Int);
        toDyn = i ?? j;

        toInt =  <error descr="Incompatible type: Float should be Int" textAttributesKey="ERRORS_ATTRIBUTES">i / j</error> ; // WRONG,  result is float
        toBool = <error descr="Unable to apply operator && for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i && j</error>; // WRONG,  no operator for int
        toBool = <error descr="Unable to apply operator || for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i || j</error>; // WRONG,  no operator for int

        toInt = <error descr="Unable to apply operator && for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i && j</error>; // WRONG,  no operator for int
        toInt = <error descr="Unable to apply operator || for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i || j</error>; // WRONG,  no operator for int
    }
    public function TestFloat(parameterA:Int, ParameterB = 0.0) {
        toFloat = parameterA + ParameterB;
        toBool = <error descr="Unable to apply operator && for types Int and Float" textAttributesKey="ERRORS_ATTRIBUTES">parameterA && ParameterB</error>;

        toFloat = f + k + i;
        toFloat = f - k - i;
        toFloat = f * k * i;
        toFloat = f / k / i;
        toFloat = f % k % i;

        toFloat = <error descr="Unable to apply operator | for types Float and Float" textAttributesKey="ERRORS_ATTRIBUTES">f |  k</error>; //WRONG, no operator for float
        toFloat = <error descr="Unable to apply operator & for types Float and Float" textAttributesKey="ERRORS_ATTRIBUTES">f &  k</error>; //WRONG, no operator for float
        toFloat = <error descr="Unable to apply operator << for types Float and Float" textAttributesKey="ERRORS_ATTRIBUTES">f << k</error>; //WRONG, no operator for float
        toFloat = <error descr="Unable to apply operator >> for types Float and Float" textAttributesKey="ERRORS_ATTRIBUTES">f >> k</error>; //WRONG, no operator for float


        toBool = f == k;
        toBool = f != k;
        toBool = f <  k;
        toBool = f <= k;
        toBool = f >  k;
        toBool = f >= k;

        toBool = (f is Float);
        toDyn = f ?? k;

        toBool = <error descr="Unable to apply operator && for types Float and Float" textAttributesKey="ERRORS_ATTRIBUTES">f && k</error>; // WRONG,  not operator for float
        toBool = <error descr="Unable to apply operator || for types Float and Float" textAttributesKey="ERRORS_ATTRIBUTES">f || k</error>; // WRONG,  not operator for float
    }

    public function TestBool() {
        toBool = <error descr="Unable to apply operator + for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a + b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator - for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a - b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator * for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a * b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator / for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a / b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator % for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a % b</error>; //WRONG, no operator for bool

        toBool = <error descr="Unable to apply operator | for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a |  b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator & for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a &  b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator << for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a << b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator >> for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a >> b</error>; //WRONG, no operator for bool


        toBool = a == b;
        toBool = a != b;

        toBool = <error descr="Unable to apply operator < for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a <  b</error>; // WRONG,  not operator for float
        toBool = <error descr="Unable to apply operator <= for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a <= b</error>; // WRONG,  not operator for float
        toBool = <error descr="Unable to apply operator > for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a >  b</error>; // WRONG,  not operator for float
        toBool = <error descr="Unable to apply operator >= for types Bool = true and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">a >= b</error>; // WRONG,  not operator for float

        toBool = (a is Bool);
        toDyn = a ?? b;

        toBool = a && b;
        toBool = a || b;


    }

    public function TestString() {
        toString = s + t;

        toBool = <error descr="Unable to apply operator - for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s - t</error>; //WRONG, no operator for String
        toBool = <error descr="Unable to apply operator * for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s * t</error>; //WRONG, no operator for String
        toBool = <error descr="Unable to apply operator / for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s / t</error>; //WRONG, no operator for String
        toBool = <error descr="Unable to apply operator % for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s % t</error>; //WRONG, no operator for String

        toBool = <error descr="Unable to apply operator | for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s |  t</error>; //WRONG, no operator for String
        toBool = <error descr="Unable to apply operator & for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s &  t</error>; //WRONG, no operator for String
        toBool = <error descr="Unable to apply operator << for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s << t</error>; //WRONG, no operator for String
        toBool = <error descr="Unable to apply operator >> for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s >> t</error>; //WRONG, no operator for String


        toBool = s == t;
        toBool = s != t;

        toBool = s <  t;
        toBool = s <= t;
        toBool = s >  t;
        toBool = s >= t;

        toBool = (a is String);
        toDyn = s ?? t;

        toBool = <error descr="Unable to apply operator && for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s && t</error>; // WRONG,  not operator for String
        toBool = <error descr="Unable to apply operator || for types String and String" textAttributesKey="ERRORS_ATTRIBUTES">s || t</error>; // WRONG,  not operator for String
    }

    public function mixedTest() {
        toDyn = null ?? t;
        toDyn = t ?? null;

        toDyn = null ?? null;

        toDyn = s.toLowerCase().length ?? i;
        toDyn = s?.toLowerCase()?.length ?? i;

        toDyn = <error descr="Unable to apply operator ?? for types String and Int" textAttributesKey="ERRORS_ATTRIBUTES">s?.toLowerCase()?.charAt(i) ?? i</error>; // WRONG, can not unify types

        toDyn = <error descr="Unable to apply operator ?? for types String and Bool = false" textAttributesKey="ERRORS_ATTRIBUTES">t ?? b</error>; // WRONG, can not unify types
        toDyn = <error descr="Unable to apply operator ?? for types String and Int" textAttributesKey="ERRORS_ATTRIBUTES">s ?? i</error>; // WRONG, can not unify types
    }
}

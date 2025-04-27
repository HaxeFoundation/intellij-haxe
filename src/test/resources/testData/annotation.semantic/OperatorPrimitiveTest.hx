package ;
// more info at https://haxe.org/manual/expression-operators-binops.html

    typedef TypeDefInt = Int;
    typedef TypeDefNullInt = Null<Int>;

class OperatorTest {
    // only available on some targets
    var si:Single = 1;

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
    // typedefs and null
    var td1:TypeDefInt = 1;
    var td2:TypeDefNullInt = 2;

    var ni:Null<Int>;
    var nd:Null<Dynamic>;
    var dynamicValue:Dynamic = 1;

    var toDyn:Dynamic;
    var toSingle:Single;
    var toInt:Int;
    var toFloat:Float;
    var toBool:Bool;
    var toString:String;

    public function new() {}
    //NOTE: single is only supported on some targets
    public function TestSingle() {

        toSingle = si + j;
        toSingle = si - j;
        toSingle = si * j;
        toSingle = si % j;


        toFloat = si / j;

        toBool = si == j;
        toBool = si != j;
        toBool = si <  j;
        toBool = si <= j;
        toBool = si >  j;
        toBool = si >= j;

        // unsupported
        toInt = <error descr="Unable to apply operator ^ for types Single and Int" textAttributesKey="ERRORS_ATTRIBUTES">si ^  j</error>;
        toInt = <error descr="Unable to apply operator | for types Single and Int" textAttributesKey="ERRORS_ATTRIBUTES">si |  j</error>;
        toInt = <error descr="Unable to apply operator & for types Single and Int" textAttributesKey="ERRORS_ATTRIBUTES">si &  j</error>;
        toInt = <error descr="Unable to apply operator << for types Single and Int" textAttributesKey="ERRORS_ATTRIBUTES">si << j</error>;
        toInt = <error descr="Unable to apply operator >> for types Single and Int" textAttributesKey="ERRORS_ATTRIBUTES">si >> j</error>;
        toInt = <error descr="Unable to apply operator >>> for types Single and Int" textAttributesKey="ERRORS_ATTRIBUTES">si >>> j</error>;


    }

    public function TestInt(parameterA:Int, ParameterB = 0) {
        toInt = parameterA + ParameterB;
        toBool = <error descr="Unable to apply operator && for types Int and Int = 0" textAttributesKey="ERRORS_ATTRIBUTES">parameterA && ParameterB</error>;

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


        toBool = ni > j;  // null<int>
        toBool = td1 > j; // typedef = int
        toBool = td2 > j; // typedef = null<Int>

        // wrong
        toInt =  <error descr="Incompatible type: Float should be Int" textAttributesKey="ERRORS_ATTRIBUTES">i / j</error> ; // WRONG,  result is float
        toBool = <error descr="Unable to apply operator && for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i && j</error>; // WRONG,  no operator for int
        toBool = <error descr="Unable to apply operator || for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i || j</error>; // WRONG,  no operator for int

        toInt = <error descr="Unable to apply operator && for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i && j</error>; // WRONG,  no operator for int
        toInt = <error descr="Unable to apply operator || for types Int and Int" textAttributesKey="ERRORS_ATTRIBUTES">i || j</error>; // WRONG,  no operator for int
    }
    public function TestFloat(parameterA:Int, ParameterB = 0.0) {
        toFloat = parameterA + ParameterB;
        toBool = <error descr="Unable to apply operator && for types Int and Float = 0.0" textAttributesKey="ERRORS_ATTRIBUTES">parameterA && ParameterB</error>;

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
        toBool = <error descr="Unable to apply operator + for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a + b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator - for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a - b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator * for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a * b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator / for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a / b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator % for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a % b</error>; //WRONG, no operator for bool

        toBool = <error descr="Unable to apply operator | for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a |  b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator & for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a &  b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator << for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a << b</error>; //WRONG, no operator for bool
        toBool = <error descr="Unable to apply operator >> for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a >> b</error>; //WRONG, no operator for bool


        toBool = a == b;
        toBool = a != b;

        toBool = <error descr="Unable to apply operator < for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a <  b</error>; // WRONG,  not operator for float
        toBool = <error descr="Unable to apply operator <= for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a <= b</error>; // WRONG,  not operator for float
        toBool = <error descr="Unable to apply operator > for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a >  b</error>; // WRONG,  not operator for float
        toBool = <error descr="Unable to apply operator >= for types Bool and Bool" textAttributesKey="ERRORS_ATTRIBUTES">a >= b</error>; // WRONG,  not operator for float

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

        toDyn ??= t;
        toFloat ??= <error descr="Incompatible type: String should be Float" textAttributesKey="ERRORS_ATTRIBUTES">t</error>; // WRONG, trying to assign incorrect type

        toDyn = null ?? null;

        toDyn = s.toLowerCase().length ?? i;
        toDyn = s?.toLowerCase()?.length ?? i;

        toFloat = s?.toLowerCase()?.length ?? i;

        toDyn = s?.toLowerCase()?.charAt(i) ?? <error descr="Incompatible type: String should be Int" textAttributesKey="ERRORS_ATTRIBUTES">i</error>; // WRONG, can not unify types

        toDyn = t ?? <error descr="Incompatible type: String should be Bool" textAttributesKey="ERRORS_ATTRIBUTES">b</error>; // WRONG, can not unify types
        toDyn = s ?? <error descr="Incompatible type: String should be Int" textAttributesKey="ERRORS_ATTRIBUTES">i</error>; // WRONG, can not unify types
    }

    public function warningOnDynamic() {
        var nullDynamic:Null<Dynamic>;
        var dynamicValue:Dynamic;
        var intValue = 1;

        toBool = <weak_warning descr="Applying > operator to a Dynamic value may cause Runtime exceptions on static targets if the value does not support the operation" textAttributesKey="INFO_ATTRIBUTES">nullDynamic > intValue</weak_warning>;
        toBool = <weak_warning descr="Applying > operator to a Dynamic value may cause Runtime exceptions on static targets if the value does not support the operation" textAttributesKey="INFO_ATTRIBUTES">dynamicValue > intValue</weak_warning>;
        toBool = <weak_warning descr="Applying > operator to a Dynamic value may cause Runtime exceptions on static targets if the value does not support the operation" textAttributesKey="INFO_ATTRIBUTES">dynamicValue > nullDynamic</weak_warning>;
        toBool = <weak_warning descr="Applying > operator to a Dynamic value may cause Runtime exceptions on static targets if the value does not support the operation" textAttributesKey="INFO_ATTRIBUTES">nullDynamic > nullDynamic</weak_warning>;
    }
}

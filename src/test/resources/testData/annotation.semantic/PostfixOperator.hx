package;

typedef SomeTD = Int;

class TestPostfix {

    public function new() {}

    public var td:SomeTD;
    public var int:Int;
    public var floatReadOnly(dynamic, never):Int;
    public var floatReadWrite(never, dynamic):Int;

    public var physical(default, set):Int;

    function set_physical(value:Int):Int {return physical++;}


    public var nonPhysical(get, set):Int;

    // EXPECT: This field cannot be accessed because it is not a real variable
    function set_nonPhysical(value:Int):Int {return <error descr="This field cannot be accessed because it is not a real variable">nonPhysical</error>++;}
    // EXPECT: This field cannot be accessed because it is not a real variable
    function get_nonPhysical():Int {return <error descr="This field cannot be accessed because it is not a real variable">nonPhysical</error>++;}


    public var intArray:Array<Int>;
    public var objArray:Array<{count:Int}>;

    @:op(A++)
    function getInt() {
        return int;
    }

    function getIntArray() {
        return intArray;
    }

    //TODO add  abstract operator overload


    public function postfix() {
        int++;
        int--;

        this.int++;
        this.int--;

        td++;
        td--;

        // EXPECT: This expression cannot be accessed for writing
        <error descr="This expression cannot be accessed for writing">floatReadOnly++</error>;
        <error descr="This expression cannot be accessed for writing">floatReadOnly--</error>;
            // EXPECT: This expression cannot be accessed for writing
        <error descr="This expression cannot be accessed for writing">this.floatReadOnly++</error>;
        <error descr="This expression cannot be accessed for writing">this.floatReadOnly--</error>;

        floatReadWrite++;
        floatReadWrite--;
        this.floatReadWrite++;
        this.floatReadWrite--;

            // EXPECT: Invalid assign
        <error descr="Invalid assign">getInt()++</error>;
        <error descr="Invalid assign">getInt()--</error>;
            // EXPECT: Invalid assign
        <error descr="Invalid assign">this.getInt()++</error>;
        <error descr="Invalid assign">this.getInt()--</error>;

        getIntArray()[0]++;
        getIntArray()[0]--;
        this.getIntArray()[0]++;
        this.getIntArray()[0]--;

        intArray[0]++;
        intArray[0]--;
        this.intArray[0]++;
        this.intArray[0]--;

            // EXPECT: Invalid assign
        <error descr="Invalid assign">this.intArray.pop()++</error>;
        <error descr="Invalid assign">this.intArray.pop()--</error>;
            // EXPECT:This expression cannot be accessed for writing
        <error descr="This expression cannot be accessed for writing">this.intArray.<error descr="Cannot access field length">length</error>++</error>;
        <error descr="This expression cannot be accessed for writing">this.intArray.<error descr="Cannot access field length">length</error>--</error>;

            // EXPECT: { count : Int } should be Int
        <error descr="{count:Int} should be Int">objArray[0]++</error>;
        <error descr="{count:Int} should be Int">objArray[0]--</error>;
            // EXPECT: { count : Int } should be Int
        <error descr="{count:Int} should be Int">this.objArray[0]++</error>;
        <error descr="{count:Int} should be Int">this.objArray[0]--</error>;

        this.objArray[0].count++;
        this.objArray[0].count--;
        this.objArray.pop().count++;
        this.objArray.pop().count--;

            // EXPECT: Invalid assign
        <error descr="Invalid assign">new TestPostfix()++</error>;
        <error descr="Invalid assign">new TestPostfix()--</error>;

        new TestPostfix().int++;
        new TestPostfix().int--;

        while (this.int++ < 1) {}
        while (this.int-- < 1) {}

        var x = 0;
        x++;
        x--;

        final z = 1;
            // EXPECT: Cannot assign to final
        <error descr="Cannot assign to immutable reference">z++</error>;
        <error descr="Cannot assign to immutable reference">z--</error>;

            // EXPECT: Invalid assign
        <error descr="Invalid assign">1++</error>;
        <error descr="Invalid assign">1--</error>;
            // EXPECT: Invalid assign
        <error descr="Invalid assign">""++</error>;
        <error descr="Invalid assign">""--</error>;

    }
}
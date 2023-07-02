package ;

import haxe.ValueException;
typedef Point<T> = { x:T, y:T };

class IsOperator extends Base {
    public static function main() {
        var v = new IsOperator();
        v.test();
    }
    public function new() { super(); }

    @:multiType(@:followWithAbstracts K)
    public function notPartOfThisTest() {

    }

    public function test() : Bool {
        var myString  = "testString";
        var priorityTest = 2 + 2 * 2 + 2 & ~0x001;
        var i : Int = 0;
        var ary : Array<String> = ["This", "That", "Other"];
        var ints : Array<Int> = [1,2,3];
        var b : Bool = false;
        var point : Point<Int> = {x:3, y:3};

        // Valid cases in 3.4.7
        var multiExpression =  (myString is String); // allowed
        var multiExpression2 = (myString is String) && false; // allowed since it has parentheses
        var multiExpression5 = true && (myString is String) || false;
        switch (myString is String) { case true: trace(true); case _: trace(false); }
        (i is String);

        // Incorrect uses before 4.2, acceptable in 4.2
#if (true || haxe_ver >= 4.1)
        var associativityTest = "test" + <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">"this" is String</error>; // 4.1: characters 49-51 : Missing ;
        var singleExpression =  <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">myString is String</error>; // 4.1: characters 42-44 : Missing ;
        var singleExpression2 = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">myString is String.String</error>; // 4.1: characters 42-44 : Missing ;
        var multiExpression3 =  <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error> || i == 1;  // must be wrapped with parentheses in 4.1, allowed in 4.2. 'Missing ;' in 3.4.
        var multiExpression3_a = i == 2 && <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error>; // 4.1:  characters 53-55 : Missing ;
        var multiExpression3_b = <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error> != true; // 4.1: characters 43-45 : Missing ;
        var multiExpression4 = true && <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error> && false;  // 4.1: Missing ; at 'is'
        var ternary1 = <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error> ? true : false; // 4.1:  characters 33-35 : Missing ;
        var ternary2 = 0 != i ? <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">b is String</error> : <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">point is String</error>; // 4.1: characters 35-37 : Expected :
        if (<error descr="\"is\" expression requires double parenthesis when used as a guard expression. (pre-4.2 semantics)">myString is String</error>) {}  // Allowed in 4.2. Syntax error in 4.1: 'characters 22-24 : Expected )'
                                    // 3.4: 'Unexpected `is`', 'Missing `;`', and 'Unexpected )'
        switch <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error> { case true: trace(true); case _: trace(false); } // 4.1: characters 25-27 : Expected {
        switch myString { case _ => a : trace(<error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">a is String</error>); } // 4.1: characters 49-51 : Expected , or )
        twoArgs(<error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">b is String</error>, <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">i is String</error>); // 4.1: characters 19-21 : Expected , or )
        if (i==9) return <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error>; // 4.1: characters 35-37 : Missing ;
        while (<error descr="\"is\" expression requires double parenthesis when used as a guard expression. (pre-4.2 semantics)">myString is String</error>) {myString = null;}  // 4.1: characters 25-27 : Expected )
        while (false) <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error>; // 4.1:  characters 32-34 : Missing ;
        do {myString = null;} while (<error descr="\"is\" expression requires double parenthesis when used as a guard expression. (pre-4.2 semantics)">myString is String</error>); // 4.1: characters 47-49 : Expected )
        do <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error> while (false); // 4.1: characters 21-23 : Expected while
        try <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is String</error> catch (s:Dynamic) trace(s); // 4.1: characters 22-24 : Missing ;
        iterate([for (a in 0...10) "item" + a], o-><error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">o is String</error>);  // 4.1: characters 54-56 : Expected , or )
        var bary = [for (a in 0...10) <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">a is String</error>]; // 4.1: characters 41-43 : Expected , or ]
        var bary = [<error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">i is String</error>];  // 4.1: characters 23-25 : Expected , or ]
        var m = [for (a in 0...10) a => <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">a is String</error>]; // 4.1: characters 43-45 : Expected , or ]
        var m = ["m" => <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">i is String</error>]; // 4.1: characters 27-29 : Expected , or ]
        untyped <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">i is String</error>; // 4.1: characters 19-21 : Missing ;
        macro <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">i is String</error>; // 4.1: characters 17-19 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">fun(i) is String</error>; // 4.1: characters 16-18 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">["This","That"][0] is String</error>; // 4.1: characters 28-30 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">"String".substr(0, 2) is String</error>; // 4.1: characters 31-33 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">123 is String</error>; // 4.1: characters 13-15 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">{x:1, y:2, z:3} is String</error>; // 4.1: characters 28-34 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">ary[0] is String</error>; // 4.1: characters 16-18 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">ary[0].substr(0,2) is String</error>; // 4.1: characters 28-30 : Missing ;
        b = <error descr="Expression on left-hand side of ''is'' operator cannot be an assignment expression. (pre-4.2 semantics)">cast("Nothing", String) is String</error>; // 4.1: characters 38-40 : Missing ;
        b = <error descr="Expression on left-hand side of ''is'' operator cannot be an assignment expression. (pre-4.2 semantics)">cast("Nothing", String).substr(0, 2) is String</error>; // 4.1: characters 51-53 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">new String("new") is String</error>; // 4.1: characters 27-29 : Missing ;
        "No" + <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">"thing" is String</error>; // 4.1: characters 26-28 : Missing ;
        if (i == 2) throw <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">new ValueException("Some exception!") is String</error>; // 4.1: characters 56-58 : Missing ;
        final fin : Bool = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">"A String" is String</error>; // 4.1: characters 39-41 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">i is String</error>; // 4.1: characters 11-13 : Missing ;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">this is String</error>; // 4.1: characters 14-16 : Missing ;

        // Acceptable in 4.2
        b = <error descr="Expression on left-hand side of ''is'' operator cannot be an assignment expression. (pre-4.2 semantics)">ints[0]++ is String</error>;
        // b = --ints[0] is String;  // compiler bug
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">point.x++ is String</error>;
        // ++point.x is String;  // compiler bug
        // b = ++i is String;  // compiler bug
        b = <error descr="Expression on left-hand side of ''is'' operator cannot be an assignment expression. (pre-4.2 semantics)">(++i) is String</error>;
        <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">i-- is String</error>;
        trace(<error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">!i is String</error>);
        // i! is String;  // Parses, but unsupported outside of macros.
#end

        // Incorrect uses in all versions.
        var incorrectUse = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">this.stage is <error descr="Right-hand side of ''is'' operator must be a Type name.">123</error></error>; //  incorrect use, Expected Type/Class got literal 'Missing ;' in 3.4.
        var incorrectUse2 = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">this.stage is <error descr="Right-hand side of ''is'' operator must be a Type name."><error descr="Type name must start by upper case">myString</error></error></error>; //  incorrect use, Expected Type/Class got variable. 'Missing ;' in 3.4.
        var incorrectUse3 = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">myString is <error descr="Unsupported type for ''is'' operator.">{x:1, y:1}</error></error>;  // Syntax error: 'Unexpected `1`.' 'Missing ;' in 3.4.
        var incorrectUse4 = <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">myString is <error descr="Unsupported type for ''is'' operator.">{x:Int, y:Int}</error></error> || call(); // 'Unsupported type for `is` operator.' 'Missing ;' in 3.4.
        var incorrectUse5 = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">myString is <error descr="Type parameters are not supported for the ''is'' operator.">Point<Int></error></error>;     // 'Type parameters are not supported for the `is` operator.' 'Missing ;' in 3.4.
        var incorrectUse6 = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">myString is <error descr="Unsupported type for ''is'' operator.">{var x:Int; var y:Int;}</error></error>  // 'Unsupported type for `is` operator.'
        var incorrectUse7 = <error descr="Unparenthesized ''is'' expression cannot be used for variable initialization. (pre-4.2 semantics)">myString is <error descr="Unsupported type for ''is'' operator.">{function iterator(){}}</error></error>  // 'Unsupported type for `is` operator.'

        if (true) {"Some" + "thing";} else {"Nothing";} is<error descr="Missing semicolon."> </error>String; // 4.2: characters 60-66 : Missing ;
        try { var s; s = "else"; } catch (e) {} is<error descr="Missing semicolon."> </error>String; // 4.2: characters 52-58 : Missing ;
        switch (ary[0]) { case _ => c: c; } is<error descr="Missing semicolon."> </error>String; // 4.2:  characters 48-54 : Missing ;

        switch myString { case _ is String: trace(true); }
        // super is String; // Parses, but cannot use super as value.

        return false;
    }

    public static function iterate(a:Array<Any>, f:Any->Bool) {
        for (o in a) f(o);
    }

    public static function fun<T>(i:T):T {return i;}

    public static function twoArgs<T>(i:T, j:T):T {return j;}

    public static function call():Bool {}

}

class Base {
    public function new() {}
}
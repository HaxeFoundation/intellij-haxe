import haxe.macro.Expr;

enum Tree<T> {
    Leaf(v:T);
    Node(l:Tree<T>, r:Tree<T>);
}

class Test {
  @:macro public static function repeat(cond:Expr, e:Expr):Expr {
    return macro while ($cond) trace($e);
  }

  @:meta(Inject())
  private var s:TextField;

  static function main() {
    var x = 0;
    repeat(x < 10, x++);
  }

  function strings() {
    var name = "Haxe";
    var x = 10;
    trace('$name is $x times better than X');
    trace('${name.toUpperCase()} is ${x*2} times better than X');
  }

  function patternMatching() {
    var myTree = Node(Leaf("foo"), Node(Leaf("bar"), Leaf("foobar")));
    var name = switch(myTree) {
        case Leaf(s): s;
        case Node(Leaf(s), _): s;
        case _: "none";
    }
    trace(name); // foo
    var myStructure = { name: "haxe", rating: "awesome" };
    var value = switch(myStructure) {
        case { name: "haxe", rating: "poor" } : throw false;
        case { rating: "awesome", name: n } : n;
        case _: "no awesome language found";
    }
    trace(value); // haxe

    var myArray = [1, 6];
    var match = switch(myArray) {
        case [2, _]: "0";
        case [_, 6]: "1";
        case []: "2";
        case [_, _, _]: "3";
        case _: "4";
    }
    trace(match); // 1
    var match = switch(7) {
        case 4 | 1: "0";
        case 6 | 7: "1";
        case _: "2";
    }
    trace(match); // 1
    var myArray = [7, 6];
    var s = switch(myArray) {
        case [a, b] if (b > a):
            b + ">" +a;
        case [a, b]:
            b + "<=" +a;
        case _: "found something else";
    }
    trace(s); // 6<=7
    var s = switch [1, false, "foo"] {
        case [1, false, "bar"]: "0";
        case [_, true, _]: "1";
        case [_, false, _]: "2";
    }
    trace(s); // 2
  }
}

@:coreType abstract Kilometer from Int {
  function foo(){}
}
class Main {
    static function main() {
        var km:Kilometer = 1;
        var n:Int = km; // Kilometer should be Int
    }
}

abstract Void { } // value type with no relations
abstract Int to Float { } // value type which implicit casts to Float
abstract UInt to Int from Int { } // value types which auto-casts to and from Int
abstract EnumFlags<T:EnumValue>(Int) { } // opaque type based on Int
abstract Vector<T>(VectorData<T>) { } // opaque type based on VectorData<T>

abstract StringSplitter(Array<String>) {
    inline function new(a:Array<String>)
        this = a

    @:from static public inline function fromString(s:String) {
        return new StringSplitter(s.split(""));
    }
}

abstract MyInt(Int) from Int to Int {
    // MyInt + MyInt can be used as is, and returns a MyInt
    @:op(A + B) static public function add(lhs:MyInt, rhs:MyInt):MyInt {}

    @:commutative @:op(A * B) static public function repeat(lhs:MyInt, rhs:String):String {
        var s:StringBuf = new StringBuf();
        for (i in 0...lhs)
            s.add(rhs);
        return s.toString();
    }
}
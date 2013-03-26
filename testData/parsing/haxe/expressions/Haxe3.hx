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

class Test {
    static function testClass() {
        var a = new MyClass(1);
        var b = new MyClass(1, "test"); // WRONG: overload not allowed for class (no annotation for now, there is not a "main" constructor so both are valid)
    }
    static function testAbstract() {
        new MyAbstract(1);
        var a = new MyAbstract(1);
        var b = new MyAbstract(1,"test");// CORRECT: overload allowed for abstract
        var c = <error descr="Too many arguments (expected 2 but got 3)\"">new MyAbstract(1,"test", true)</error>;// WRONG: no overload with these params
    }
}


class MyClass {
    // WRONG: overload not allowed for class constructor
    public inline extern <error descr="Invalid modifier: overload on constructor">overload</error> function new(a: Int) {}
public inline extern <error descr="Invalid modifier: overload on constructor">overload</error> function new(a: Int, b: String) {}
}

abstract MyAbstract(Int) {
public inline extern overload function new (a:Int) { this = a; }
public inline extern overload function new(a:Int, b:String) {this = a; }
}

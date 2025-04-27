class TypeTagClass {

    <error descr="Variable 'x' requires type-hint or initialization">final x;</error> //Variable requires type-hint or initialization
    <error descr="Variable 'y' requires type-hint or initialization">var y;</error> // Variable requires type-hint or initialization

    final a:Int; // correct
    var b:Int; // correct

    var p(default, default):Int; // correct
    <error descr="Variable 'q' requires type-hint or initialization">var q(default, default);</error> // Variable requires type-hint or initialization

    public function new() {
        var localVar; // hint not required
        x = 1;
        y = 1;
        a = 1;
        b = 1;
    }
}

interface TypeTagInterface {
    <error descr="Variable 'x' requires type-hint or initialization">final x;</error> //Variable requires type-hint or initialization
    <error descr="Variable 'y' requires type-hint or initialization">var y;</error> // Variable requires type-hint or initialization

    final a:Int; // correct
    var b:Int; // correct

    var p(default, default):Int; // correct
    <error descr="Variable 'q' requires type-hint or initialization">var q(default, default);</error> // Variable requires type-hint or initialization
}

class NullCoalescingTest {
    public function voidFunction(): Void {}
    public function stringFunction(): Null<String> {return null;}
    public function boolFunction(): Null<Bool> {return null;}

    public function testCase(): Void {
        var testVar:Dynamic;

        testVar = stringFunction() ?? " default"; // Correct

        testVar = stringFunction() ?? <error descr="Cannot use Void as value">voidFunction()</error>; // Incorrect: Can not use void as value
        testVar = <error descr="Cannot use Void as value">voidFunction()</error> ?? stringFunction(); // Incorrect: Can not use void as value

        testVar = stringFunction() ?? <error descr="Incompatible type: Null<String> should be Null<Bool>">boolFunction()</error>; // Incorrect: different types (can not unify)
        testVar = boolFunction() ?? <error descr="Incompatible type: Null<Bool> should be Null<String>">stringFunction()</error>; // Incorrect: different types (can not unify)
    }
}
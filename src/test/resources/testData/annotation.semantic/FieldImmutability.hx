class FinalTest {
    final x = 234;
    var y = x;
    public function new() {
        y = 1; // allowed
        <error descr="Cannot assign value to 'final' variable.">x = 1</error>; // not allowed
    }
}

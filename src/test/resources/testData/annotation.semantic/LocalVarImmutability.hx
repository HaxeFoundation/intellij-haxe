class FinalTest {
    public function new() {

        final x = 234;
        var y = x;

        y = 1; // allowed
        <error descr="Cannot assign value to 'final' variable.">x = 1</error>; // not allowed

    }
}

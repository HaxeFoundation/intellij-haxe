package;
class Test {
    function new() {
        // Default name is x (when everything else fails), but we don't want a duplicate.
        var x = false;
        var x1 = 1.0 + 2.0;
        var y = x1 + Std.int("3.0");
    }
}
package;
class Test {
    function new() {
        var keys = ["1" => 1, "2" => 2].keys();
        for (i in keys) Sys.println(i);
    }
}
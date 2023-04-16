package ;
import haxe.Timer;
// This tests http:github.com/haxefoundation/intellij-haxe/issues/544
// which was the inability to parse a function call immediately after an anonymous
// function was defined.
class TestIssue544 {
    public function new() {
        // Error markers used to appear here ------------------------------------------------+
        //                          and here ---------------------------------------------+  |
        //                                                                                v  v
        var a = Timer.delay(function(c){return (function(){trace("Haxe is great!", c);});}(3), 1000);
        // This should parse cleanly and not be in a dummy block.
        var v : Array<String> = {}
    }
}

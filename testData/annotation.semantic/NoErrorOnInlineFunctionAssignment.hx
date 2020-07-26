package ;
import haxe.Constraints.Function;

class TestIssue773 {
    static function main() {
        trace("Haxe is great!");
        var callback:Function = function():String {
            return "test";
        };
    }
}
package ;

class Test {
    static function main() {
        function is_c(val) { return val == "c"; }

        trace(ArrayUtils.delete_if([ "a", "b", "c" ], is_c));
    }
}
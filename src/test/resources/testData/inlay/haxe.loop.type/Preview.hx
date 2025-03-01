class Test {
    static function main() {
        var array = ["A", "B", "C"];
        for (myChar/*<# :|String #>*/ in array) {
            trace("myChar");
        }
    }
}
class Test {

    public static function printText(value:String,
                                     ?times:Int = 1) {
        for (t in 0...times) {
            trace(value);
        }
    }

    static function main() {
        var string = "world";
        var multiplier = 20;
        printText(string, multiplier + 5);
    }
}
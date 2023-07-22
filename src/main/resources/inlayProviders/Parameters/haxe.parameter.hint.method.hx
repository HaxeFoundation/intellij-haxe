class Test {

    public static function printText(word:String,
                                     ?times:Int = 1) {
        for (t in 0...times) {
            trace(word);
        }
    }

    static function main() {
        printText("hello", 2);
    }
}
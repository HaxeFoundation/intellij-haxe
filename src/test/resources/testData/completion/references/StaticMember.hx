class StaticMethods {
    public static function foo() {}
}

class Main {
    public static function main() {
        StaticMethods.<caret>
    }
}
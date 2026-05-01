class Main {
    public static function main() {
        f<caret>
    }
    // this should be in suggestions as its accessible
    private static function foo() {}
}
class OtherClass {
    // this should not be in suggestions
    private static function bar() {}
}
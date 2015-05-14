enum FooEnum
{
    FIRST;
    SECOND;
}

class StaticMember {
    public static function foo() {}
    public static var bar;
}

class Main {
    public static function main() {
        StaticMember.<caret>
    }
}
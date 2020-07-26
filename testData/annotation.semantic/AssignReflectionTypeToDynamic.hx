package;
class Test {
    static function main() {
        trace("Haxe is great!");

        var instance:Dynamic = Type.createInstance(Test, []);
    }
}
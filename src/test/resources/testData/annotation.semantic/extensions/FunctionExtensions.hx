package extensions;
class FunctionExtensions {
    public static function testExtension(x:Int->Int, y:Int) {
        return String.fromCharCode(x(y));
    }
}
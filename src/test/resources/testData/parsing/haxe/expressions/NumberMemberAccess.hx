using NumberMemberAccess;
class NumberMemberAccess {
    static function main() {
        var doubled = 1.double();
        var zero = 0.double();
        var separated = 1_0.double();
        var hex = 0x10.double();
        var halved = 1.5.half();
        var exponent = 1.e5.half();
        var uppercase = 1.E5;
        var trailingDot = (1.);
        for (i in 0...10) {}
    }
    static function double(i:Int) return i * 2;
    static function half(f:Float) return f / 2;
}

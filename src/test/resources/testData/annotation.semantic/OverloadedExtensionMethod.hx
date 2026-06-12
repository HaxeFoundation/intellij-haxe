using extensions.MapToolsExtensions;

class Test {
    // pickValue has Float and Int overloads, the Float one declared first;
    // the receiver decides which one applies
    static function calculate(groups:Map<String, Map<String, Int>>, ratios:Map<String, Float>) {
        // Int overload applies to a Map<String, Int> receiver, so assigning to an Int var is valid
        var innerValue = 0;
        innerValue = groups.get("outer").pickValue("inner");

        // Float overload applies to a Map<String, Float> receiver
        var fromRatios:Float = ratios.pickValue("a");

        // declaration-site inference must pick the Int overload as well
        var inferred = groups.get("outer").pickValue("inner");
        var asInt:Int = inferred;
    }

    static function main() {}
}

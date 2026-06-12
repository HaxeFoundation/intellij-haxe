using extensions.MapToolsExtensions;

class Test {
    // mirrors a real-world battle damage calculation: getOrZero has Float and Int overloads,
    // the Float one declared first; the receiver decides which one applies
    static function calculate(defenseBonus:Map<String, Map<String, Int>>, floatMap:Map<String, Float>) {
        // Int overload applies to a Map<String, Int> receiver, so assigning to an Int var is valid
        var defTerrainModifier = 0;
        defTerrainModifier = defenseBonus.get("terrains").getOrZero("forest");

        // Float overload applies to a Map<String, Float> receiver
        var fromFloatMap:Float = floatMap.getOrZero("a");

        // declaration-site inference must pick the Int overload as well
        var attTerrainModifier = defenseBonus.get("terrains").getOrZero("forest");
        var asInt:Int = attTerrainModifier;
    }

    static function main() {}
}

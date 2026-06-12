package ;
// expected types verified against the Haxe compiler (4.3.7): overloads are selected
// by best argument match, not declaration order (the Float overload is declared first on purpose)

class SimpleOverloads {
    public static extern inline overload function pick(v:Float):Float {
        return v;
    }

    public static extern inline overload function pick(v:Int):Int {
        return v;
    }
}

abstract FrozenMap<K, V>(Map<K, V>) from Map<K, V> {
    public inline function get(k:K):Null<V> {
        return this.get(k);
    }
}

class MapToolsX {
    public static extern inline overload function pickValue<K>(map:FrozenMap<K, Float>, key:K):Float {
        return 0.0;
    }

    public static extern inline overload function pickValue<K>(map:FrozenMap<K, Int>, key:K):Int {
        return 0;
    }
}

class OverloadStaticCallHints {
    public function new(intMap:Map<String, Int>, floatMap:Map<String, Float>, nested:Map<String, Map<String, Int>>) {
        var fromIntArg/*<# :|Int #>*/ = SimpleOverloads.pick(1);
        var fromFloatArg/*<# :|Float #>*/ = SimpleOverloads.pick(1.5);

        var fromIntMap/*<# :|Int #>*/ = MapToolsX.pickValue(intMap, "k");
        var fromFloatMap/*<# :|Float #>*/ = MapToolsX.pickValue(floatMap, "k");
        var fromNestedAccess/*<# :|Int #>*/ = MapToolsX.pickValue(nested.get("outer"), "inner");
    }
}

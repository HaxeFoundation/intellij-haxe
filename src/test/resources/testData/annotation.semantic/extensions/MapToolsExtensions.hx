package extensions;

abstract ReadOnlyMap<K, V>(Map<K, V>) from Map<K, V> {
    public inline function get(k:K):Null<V> {
        return this.get(k);
    }
}

class MapToolsExtensions {
    public static extern inline overload function getOrZero<K>(map:ReadOnlyMap<K, Float>, key:K):Float {
        return getOrDefault(map, key, 0.0);
    }

    public static extern inline overload function getOrZero<K>(map:ReadOnlyMap<K, Int>, key:K):Int {
        return getOrDefault(map, key, 0);
    }

    public static inline function getOrDefault<K, V>(map:ReadOnlyMap<K, V>, key:K, defaultValue:V):V {
        var value = map.get(key);
        return if (value != null) value else defaultValue;
    }
}

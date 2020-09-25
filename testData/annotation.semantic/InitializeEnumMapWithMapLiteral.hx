package;

import EnumValue;
import haxe.ds.EnumValueMap;

enum MyEnum {
    FIRST;
    SECOND;
    THIRD;
}

class InitializeEnumMapWithMapLiteral {
    private var map = [ FIRST => "first", SECOND => "second" ];
    private var enummap:EnumValueMap<MyEnum, String> = [ FIRST => "first", SECOND => "second" ];
    private var mapv:Map<EnumValue, String> = [ FIRST => "first", SECOND => "second" ];
    private var mape:Map<MyEnum, String> = [ FIRST => "first", SECOND => "second" ];

    static public function main() {
        var t = new InitializeEnumMapWithMapLiteral();
        var m : Map<MyEnum, String> = t.map;

        t.enummap = m;
        t.enummap = t.map;
        t.enummap = t.mape;
        t.mape = m;
        t.map = t.mape;
        t.mape = t.map;

        // Errors below here...
        t.mapv = m;
        t.map = t.enummap;
        t.mapv = t.enummap;
        t.enummap = t.mapv;
        t.map = t.mapv;
        t.mapv = t.map;
    }

    public function new() {
        $type(map);     // haxe.ds.Map<MyEnum, String>
        $type(enummap); // haxe.ds.EnumValueMap<MyEnum, String>
        $type(mapv);    // Map<EnumValue, String>
        $type(mape);    // Map<MyEnum, String>
        map.set(THIRD, "third");
        enummap.set(THIRD, "third");
        mapv.set(THIRD, "third");
        mape.set(THIRD, "third");
    }
}

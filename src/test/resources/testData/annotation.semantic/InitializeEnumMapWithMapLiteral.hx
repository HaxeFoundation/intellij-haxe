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

        // the haxe.ds.Map<> type is an abstract "multiType", assign operations are based on the "@:to/@:from" functions

        t.enummap = m;
        t.enummap = t.map;
        t.enummap = t.mape;
        t.mape = m;
        t.map = t.mape;
        t.mape = t.map;

        enumValueParameter(t.mapv);
        enumValueConstraint(t.mapv);
        enumValueConstraint(t.mape);

        // Errors below here...
        t.mapv = <error descr="Incompatible type: Map<MyEnum, String> should be Map<EnumValue, String>">m</error>;
        t.map = <error descr="Incompatible type: EnumValueMap<MyEnum, String> should be haxe.ds.Map<MyEnum, String>">t.enummap</error>;
        t.mapv = <error descr="Incompatible type: EnumValueMap<MyEnum, String> should be Map<EnumValue, String>">t.enummap</error>;
        t.enummap = <error descr="Incompatible type: Map<EnumValue, String> should be EnumValueMap<MyEnum, String>">t.mapv</error>;
        t.map = <error descr="Incompatible type: Map<EnumValue, String> should be haxe.ds.Map<MyEnum, String>">t.mapv</error>;
        t.mapv = <error descr="Incompatible type: haxe.ds.Map<MyEnum, String> should be Map<EnumValue, String>">t.map</error>;

        enumValueParameter(<error descr="Type mismatch (Expected: 'Map<EnumValue, String>' got: 'Map<MyEnum, String>')">t.mape</error>);


    }
    // method parmeter of with typeparameter of EnumValue can not accept map/Array of spesifict Enum,
    // however if EnumValue  is used as a constraint for typeParameter/generics it should pass.
    static function enumValueConstraint<T:EnumValue>(x:Map<T, String>) {}
    static function enumValueParameter(x:Map<EnumValue, String>) {}

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

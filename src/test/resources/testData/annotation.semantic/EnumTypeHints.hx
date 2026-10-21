class LooksLikeClass {}

enum EnumForHints {
    LooksLikeClass;
    NotClass;
}

enum EnumForExtractorHints<T, Q> {
    ExtractableEnum(x:T);
    ExtractableEnum2(x:T, y:Q);
}

enum MapKeyEnum {
    SharedName;
    KeyOnly;
}

enum MapValueEnum {
    SharedName;
    ValueOnly;
}

class TestAssignHints {
    public function new() {
        // normal assign hinting
        var fullyQualified:EnumForHints = EnumForHints.LooksLikeClass;
        var hintFromType:EnumForHints = NotClass;
        var competingClassName:EnumForHints = LooksLikeClass;
        var className:Class<Dynamic> = LooksLikeClass;

        // not found
        var notFound:EnumForHints = <warning descr="Unresolved symbol">NotFound</warning>;
        // wrong type
        var wrong:EnumForHints = <error descr="Incompatible type: Class<TestAssignHints> should be EnumForHints">TestAssignHints</error>;

        // verify resolve from usage:
        var mapKeys:Map<MapKeyEnum, Int> = [SharedName => 1, KeyOnly => 2];
        var mapValues:Map<Int, MapValueEnum> = [1 => SharedName, 2 => ValueOnly];
        var mapBoth:Map<MapKeyEnum, MapValueEnum> = [SharedName => SharedName, KeyOnly => ValueOnly];
        var intMapValues:haxe.ds.IntMap<MapValueEnum> = [1 => SharedName, 2 => ValueOnly];

        var mapKeyNotFound:Map<MapKeyEnum, Int> = [<warning descr="Unresolved symbol">NotFound</warning> => 1];

        // enum switch extractor hinting

        var str:String;
        var num:Int;
        var flt:Float;

        var enumVarA = ExtractableEnum("StringVal");
        var enumVarB = ExtractableEnum2(1,2);
        var enumVarC  = ExtractableEnum2(1, enumVarA);

        switch (enumVarA) {
            case ExtractableEnum(myVal) : str = myVal;
        }

        switch ({a :enumVarA}) {
            case {a : ExtractableEnum(myValA  )} :
                {
                    str = myValA; // correct

                    // wrong type
                    flt = <error descr="Incompatible type: String should be Float">myValA</error>;
                }
        }

        switch (enumVarC) {
            case ExtractableEnum2(myIntVal, ExtractableEnum(myStrVal)) :
                {
                    num =  myIntVal; // correct
                    str =  myStrVal; // correct

                    str = <error descr="Incompatible type: Int should be String">myIntVal</error>; // wrong type
                    flt = <error descr="Incompatible type: String should be Float">myStrVal</error>; // wrong type
                }
        }

        switch ([enumVarA, enumVarB]) {
            case [ExtractableEnum(myValA), ExtractableEnum(myValB) ]:
                {
                    str = myValA; // correct
                    num = myValB; // correct

                    flt = <error descr="Incompatible type: String should be Float">myValA</error>; // wrong type
                }
        }
        switch ({a :[enumVarA, enumVarB]}) {
            case {a : [ExtractableEnum(myValA ), ExtractableEnum(myValB) ]}:
                {
                    str = myValA; // correct
                    num = myValB; // correct

                    flt = <error descr="Incompatible type: String should be Float">myValA</error>; // wrong type
                }
        }
    }
}

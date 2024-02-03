package;

import test.EnumWithClassNameValues;

class Test1147 {
    public function new() {
        // should resolve to normal Array class
        var array:Array<String> = [] ;
        // should use enum values
        var enumValA:EnumWithClassNameValues<String> = Array;
        var enumValB = EnumWithClassNameValues.Array;
        var enumValC = EnumWithClassNameValues.String;
    }
}

enum NoConstEnum {
    ValueA;
    ValueB;
    ValueC;
}

enum ConstEnum {
    ValueA(x:String);
    ValueB(x:Int);
    ValueC(x:Bool);
}

class SameNameEnumValuesAndConstructors {
    public function testReturn(val:NoConstEnum) {
        return  switch (val) {
            case ValueA : ValueA("test"); // correct params
            case ValueB : ValueB(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"test"</error>); // wrong params
            case ValueC : ValueC<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; // missing params
        }
    }
    public function testAssing(val:NoConstEnum) {
        var mapped:ConstEnum;
        switch (val) {
            case ValueA : mapped = ValueA("test"); // correct params
            case ValueB : mapped =  ValueB(<error descr="Type mismatch (Expected: 'Int' got: 'String')">"test"</error>); // wrong params
            case ValueC : mapped =  ValueC<error descr="Not enough arguments (expected 1 but got 0)\"">()</error>; // missing params
        }
    }
}

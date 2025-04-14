enum MyEnum {
    someValue(value:String);
    otherValue;
}

class Test {
    static function main() {
        var myVal = otherValue;
        switch (myVal) {
            case someValue(value): trace(value);
            case var x/*<# :|MyEnum #>*/: trace("enum  is" + x.getName());
        }
    }
}
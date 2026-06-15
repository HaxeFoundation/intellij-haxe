class ObjectLiteralKeyExtends {
    public static function main() {
        var widget:Widget = {
            la<caret>bel: "value",
            width: 1,
        };
    }
}

typedef Base = {
    var label:String;
}

typedef Widget = {
    > Base,
    var width:Int;
}

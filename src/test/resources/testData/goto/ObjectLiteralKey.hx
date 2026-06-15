class ObjectLiteralKey {
    public static function main() {
        var config:Config = {
            na<caret>me: "value",
            size: 1,
        };
    }
}

typedef Config = {
    var name:String;
    var size:Int;
}

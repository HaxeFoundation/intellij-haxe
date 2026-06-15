class ObjectLiteralKeyIntersection {
    public static function main() {
        var combo:Combo = {
            al<caret>pha: 1,
            beta: "value",
        };
    }
}

typedef Combo = {alpha:Int} & {beta:String};

class TypeFromObjectLiteral {
    public function new() {
        var x:{a:String -> Void, s:String};
        x.a = myMethod<caret>;
    }
}

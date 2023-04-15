class NamedNestedFunction {
    public function new() {
        var names = Reflect.fields(mJson);
        names.sort(function compare(x : String, y : String) : Int {
            return (x > y) ? 1 : (x < y) ? -1 : 0;
        });
        return names;
    }
}
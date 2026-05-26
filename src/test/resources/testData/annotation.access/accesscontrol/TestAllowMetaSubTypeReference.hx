package accesscontrol;
// Regression: @:allow argument referencing a sub-type inside a multi-class
// module uses the package-elided form (accesscontrol.AllowedSubType) that the
// Haxe compiler accepts. The IDE must not flag this with "Unresolved symbol".

class TestAllowMetaSubTypeReference {
    @:allow(accesscontrol.AllowedSubType)
    private var secret:Int = 0;

    public function new() {}
}

class AllowedSubType {
    public function new() {}

    public function read(target:TestAllowMetaSubTypeReference):Int {
        return target.secret;
    }
}

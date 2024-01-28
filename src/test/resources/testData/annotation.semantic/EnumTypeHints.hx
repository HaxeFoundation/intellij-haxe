class LooksLikeClass {}

enum EnumForHints {
    LooksLikeClass;
    NotClass;
}

class TestAssignHints {
    public function new() {
        var fullyQualified:EnumForHints = EnumForHints.LooksLikeClass;
        var hintFromType:EnumForHints = NotClass;
        var competingClassName:EnumForHints = LooksLikeClass;
        var className:Class<Dynamic> = LooksLikeClass;

            // not found
        var notFound:EnumForHints = <warning descr="Unresolved symbol">NotFound</warning>;
            // wrong type
        var <error descr="Incompatible type: Class<TestAssignHints> should be EnumForHints">wrong:EnumForHints = TestAssignHints</error>;
    }
}

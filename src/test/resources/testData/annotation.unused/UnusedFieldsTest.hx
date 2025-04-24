class UnusedFieldsTest {
    public static var unusedStaticPublic = 1;// no annotation expected
    private static var <warning descr="Unused variable">unusedStaticPrivate</warning> = 1;
    static var <warning descr="Unused variable">unusedStaticDefault</warning> = 1;

    public final unusedFinalPublic = 1;// no annotation expected
    private final <warning descr="Unused variable">unusedFinalPrivate</warning> = 1;
    final <warning descr="Unused variable">unusedFinalDefault</warning> = 1;

    public var unusedPublic = 1;// no annotation expected
    private var <warning descr="Unused variable">unusedPrivate</warning> = 1;
    var <warning descr="Unused variable">unusedDefault</warning> = 1;

    private var usedPrivate = 1;// no annotation expected
    var usedDefault = 1; // no annotation expected

    private var usedPrivateOtherClass = 1;// no annotation expected
    var usedOtherClass = 1; // no annotation expected

    private var usedPrivateOutside = 1;// no annotation expected
    var usedOutside = 1; // no annotation expected

    public function new() {
        var inuse = 1;
        var <warning descr="Unused variable">unused</warning> = 1;
        if(inuse == usedPrivate) {
            return usedDefault;
        }

    }
}
class UnusedTestEx {
    public function new() {
        var value:UnusedFieldsTest;

        @:privateAccess
        if(value.usedPrivateOtherClass == value.usedOtherClass) {
            return 1;
        }

    }
}
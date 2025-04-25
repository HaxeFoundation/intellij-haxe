class UnusedFieldsAndVariablesTest {
    public static var unusedStaticPublic = 1;// no annotation expected
    private static var <warning descr="Field 'unusedStaticPrivate' is never used">unusedStaticPrivate</warning> = 1;
    static var <warning descr="Field 'unusedStaticDefault' is never used">unusedStaticDefault</warning> = 1;

    public final unusedFinalPublic = 1;// no annotation expected
    private final <warning descr="Field 'unusedFinalPrivate' is never used">unusedFinalPrivate</warning> = 1;
    final <warning descr="Field 'unusedFinalDefault' is never used">unusedFinalDefault</warning> = 1;

    public var unusedPublic = 1;// no annotation expected
    private var <warning descr="Field 'unusedPrivate' is never used">unusedPrivate</warning> = 1;
    var <warning descr="Field 'unusedDefault' is never used">unusedDefault</warning> = 1;

    private var usedPrivate = 1;// no annotation expected
    var usedDefault = 1; // no annotation expected

    private var <warning descr="Field 'usedPrivateOtherClass' is never used">usedPrivateOtherClass</warning> = 1;// no annotation expected
    var <warning descr="Field 'usedOtherClass' is never used">usedOtherClass</warning> = 1; // no annotation expected

    private var usedPrivateOutside = 1;// no annotation expected
    var usedOutside = 1; // no annotation expected

    public function new() {
        var inuse = 1;
        var <warning descr="Variable 'unused' is never used">unused</warning> = 1;
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
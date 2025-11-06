class SwitchCase {
    function switchCase() {
        var array<caret> = [
            1,
            2,
            3
        ];
        switch (true) {
            case true: // test normal scope
            case false: { // test with scope
            }
            case null: // thest child scope
                if (true) {
                }
            default :
        }
    }
}

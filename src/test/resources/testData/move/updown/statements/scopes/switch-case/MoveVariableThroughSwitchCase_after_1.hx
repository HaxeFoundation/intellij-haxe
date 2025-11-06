class SwitchCase {
    function switchCase() {
        switch (true) {
            case true: // test normal scope
            case false: { // test with scope
                var array = [
                    1,
                    2,
                    3
                ];
            }
            case null: // thest child scope
                if (true) {
                }
            default :
        }
    }
}

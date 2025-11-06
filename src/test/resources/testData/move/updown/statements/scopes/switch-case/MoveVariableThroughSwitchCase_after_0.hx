class SwitchCase {
    function switchCase() {
        switch (true) {
            case true: // test normal scope
                var array = [
                    1,
                    2,
                    3
                ];
            case false: { // test with scope
            }
            case null: // thest child scope
                if (true) {
                }
            default :
        }
    }
}

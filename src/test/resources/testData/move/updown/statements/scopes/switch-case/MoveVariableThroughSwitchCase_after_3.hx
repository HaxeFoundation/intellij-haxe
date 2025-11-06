class SwitchCase {
    function switchCase() {
        switch (true) {
            case true: // test normal scope
            case false: { // test with scope
            }
            case null: // thest child scope
                var array = [
                    1,
                    2,
                    3
                ];
                if (true) {
                }
            default :
        }
    }
}

class SwitchCase {
    function switchCase() {
        switch (10) {
            case 1: // test normal scope
            case 2: { // test with scope
                if (true) {
                    var i = 1;
                } else {
                    trace(2);
                }
            }
            case 3: // thest child scope
                if (true) {
                }
            default :
        }
    }
}

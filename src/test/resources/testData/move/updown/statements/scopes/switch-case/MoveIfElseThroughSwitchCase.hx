class SwitchCase {
    function switchCase() {
        if (true)<caret> {
            var i = 1;
        } else {
            trace(2);
        }
        switch (10) {
            case 1: // test normal scope
            case 2: { // test with scope
            }
            case 3: // thest child scope
                if (true) {
                }
            default :
        }
    }
}

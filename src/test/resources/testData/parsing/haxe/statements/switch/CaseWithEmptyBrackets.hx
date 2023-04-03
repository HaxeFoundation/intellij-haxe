class EmptySwitchCase {
    public function new() {
        var a = true;
        switch(a) {
            case true:
                {
                    trace("true");
                }
            case false:
                {
                    // do nothing
                }
        }
    }
}

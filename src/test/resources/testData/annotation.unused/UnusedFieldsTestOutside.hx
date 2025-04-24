class UnusedFieldsTestOutside {
    public function new() {
        var value:UnusedFieldsTest;
        @:privateAccess
        if(value.usedPrivateOutside == value.usedOutside) {
            return 1;
        }
    }
}

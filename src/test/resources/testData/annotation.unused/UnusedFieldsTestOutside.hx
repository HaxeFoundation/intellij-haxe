class UnusedFieldsTestOutside {
    public function new() {
        var value:UnusedFieldsAndVariablesTest;
        @:privateAccess
        if(value.usedPrivateOutside == value.usedOutside) {
            return 1;
        }
    }
}

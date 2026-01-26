class  LastWithDefault<A, B, C = String> {
    public function new() {
        // OK
        var none = new LastWithDefault(); // allowed, typeParams will be determend by useage
        var useDefault= new LastWithDefault<String, Int>();
        var overrideDefault = new LastWithDefault<String, Int, Bool>();

        // not allowed
        var tooFew = new <error descr="Invalid number of type parameters for LastWithDefault (expected: 2 got: 1)">LastWithDefault<Int></error>();
        var tooMany = new <error descr="Invalid number of type parameters for LastWithDefault (expected: 3 got: 4)">LastWithDefault<Int, Int, Int, Int></error>();
    }
}
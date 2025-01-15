class ParameterMonomorph {

    public function new() {
        var test/*<# :String #>*/  = testFunction(null);
    }
    function testFunction(?p)/*<# :String #>*/  {
        if (p == null) {
            p = "string";
        }
        return p;
    }
}
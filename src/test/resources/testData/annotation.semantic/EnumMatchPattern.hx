package;

enum TestEum {
    ValueA;
    ValueB;
    constructorA(x:Int);
    constructorB(x:String);
}

class EnumPattern {
    public function new() {
        var valA:TestEum = ValueA;
        var valB:TestEum = ValueB;

        // Correct
        var match = valA.match(ValueA  | ValueB);
        var match = ValueA.match(ValueA  | ValueB);
        var match = TestEum.ValueA.match(ValueA | ValueB);
        var match = constructorA(1).match(ValueA  | ValueB);

        var match = valA.match(constructorA(_) | ValueB);
        var match = valA.match(constructorA(_)  | constructorB(_));
        var match = valA.match(ValueA | constructorA(_) |  ValueB | constructorB(_));

        // wrong
        var match = <error descr="Unable to apply operator | for types ValueA and ValueB">ValueA |  ValueB</error>; // pattern not allowed outside match
        var match = ValueA.match(<error descr="Unable to apply operator & for types ValueA and ValueB">ValueA  & ValueB</error>); // && not allowed in pattern
        var match = ValueA.match(<error descr="Unable to apply operator | for types ValueA and TestEum">ValueA  | valB</error>); // variables not allowed in patterns


        //TODO this should fail
        var match = constructorA.match (ValueA  | ValueB); // constructorA should be a function type (x : Int) -> TestEum
    }
}

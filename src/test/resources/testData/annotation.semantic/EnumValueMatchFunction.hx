enum TestEnumForMatcher {
    EmptyA;
    EmptyB;
    OneArgument(x:String);
    TwoArguments(x:String, y:Int);
}

class EnumValueMatcherTest {
    public function new() {
        var type:TestEnumForMatcher;

        //Correct
        type.match(EmptyA);
        type.match(EmptyA | EmptyB);
        type.match(EmptyA | OneArgument(_));
        type.match(OneArgument(_ => "String"));
        type.match(TwoArguments(_, _));
        type.match(TwoArguments(_));// OK: not required to include all arguments

        // WRONG
        //TODO make sure these get error annotations
        type.match(TwoArguments);// WRONG: TwoArguments is constructor and needs arguments
        type.match(TwoArguments<error descr="Not enough arguments (expected 2 but got 0)\"">()</error>);// WRONG: minimum 1 argument required
        type.match(TwoArguments(_, _, _)); // WRONG: too many arguments
        type.match(TwoArguments(<warning descr="Unresolved symbol">t</warning>, <warning descr="Unresolved symbol">q</warning>)); //WRONG: Pattern variables are not allowed in .match patterns

        someFunction(OneArgument(_ => "string"));// WRONG
        type.match(OneArgument(_ => 1)); //WRONG, type missmatch string expected ogt int

    }

    function someFunction(x:Dynamic) {}

}

<error descr="Invalid package name! 'enumMatcher' should be ''">package enumMatcher;</error>

enum TestEnumForMatcher<T> {
    EmptyA;
    EmptyB;
    OneArgument(x:String);
    optionalArgument(?x:String);
    TwoArguments(x:String, y:Int);
    GenericArgument(x:T);
}

enum abstract AbstractEnum {
    var EnumField = 1;
}

class EnumValueMatcherTest {
    function someFunction(x:Dynamic) {}

    public function test() {
        var type:TestEnumForMatcher<String>;

        //Correct
        type.match(EnumField) ;
        type.match(EmptyA);
        type.match(EmptyA | EmptyB);
        type.match(EmptyA | OneArgument(_));
        type.match(OneArgument(_));
        type.match(optionalArgument());
        type.match(optionalArgument(_));
        type.match(OneArgument(_ => "String"));
        type.match(TwoArguments(_, _));
        type.match(TwoArguments(_.toLowerCase()=> "string", _));
        type.match(TwoArguments(_));// OK: not required to include all arguments

            // WRONG
        type.match(<error descr="Unable to apply operator & for types EmptyA<T> and EmptyB<T>">EmptyA & EmptyB</error>); //WRONG: Unrecognized pattern: EmptyA & EmptyB
        type.match(<error descr="Invalid match: Not enough patterns">TwoArguments</error>);// WRONG: TwoArguments is constructor and needs arguments
        type.match(TwoArguments<error descr="Not enough arguments (expected 2 but got 0)\"">()</error>);// WRONG: minimum 1 argument required
        type.match(TwoArguments(<error descr="Too many arguments (expected 2 but got 3)\"">_, _, _</error>)); // WRONG: too many arguments
        someFunction(OneArgument(<error descr="Pattern matching not allowed here">_ => "string"</error>));// WRONG: pattern matching not allowed  outside .match and switch-case

            // TODO 1 : Error for Pattern variables
        type.match(TwoArguments(<warning descr="Unresolved symbol">t</warning> , <warning descr="Unresolved symbol">q</warning>)); //WRONG: Pattern variables are not allowed in .match patterns

            // TODO 2: type checking
        type.match(OneArgument(_ => 1)); //WRONG, type missmatch string expected ogt int

            //TODO 3: handle variables/fields with underscore name (underscore is valid variable/ field name)
        var _ = 123;
        var z = OneArgument(<error descr="Type mismatch (Expected: 'String' got: 'Int')">_</error>);// wrong : argument should be String not int
        var z:TestEnumForMatcher<String> = GenericArgument(_);// Wrong argument is Int  does not match generics
    }
}
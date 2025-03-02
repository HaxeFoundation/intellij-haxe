package ;

class AssignArrayLiteral {
    var fieldColor:Array<TestColor>;
    var propertyColor(default, default):Array<TestColor>;

    var fieldInt:Array<Int>;
    var propertyInt(default, default):Array<Int>;

    public function new() {
        var variableColor:Array<TestColor>;
        var variableInt:Array<Int>;
        // correct
        variableColor = [0xFF008000, 0xFFFF0000];
        fieldColor = [0xFF008000, 0xFFFF0000];
        propertyColor = [0xFF008000, 0xFFFF0000];

        fieldInt = fieldColor;
        propertyInt = propertyColor;
        variableInt = variableColor;

        argumentTest(variableColor, fieldColor, propertyColor);
        argumentTest(variableInt, fieldInt, propertyColor);

        //wrong
        variableColor = <error descr="Incompatible type: Array<String> should be Array<TestColor>">["0xFF008000", "0xFFFF0000"]</error>;
        fieldColor = <error descr="Incompatible type: Array<String> should be Array<TestColor>">["0xFF008000", "0xFFFF0000"]</error>;
        propertyColor = <error descr="Incompatible type: Array<String> should be Array<TestColor>">["0xFF008000", "0xFFFF0000"]</error>;

    }

    public function argumentTest(x:Array<TestColor>, y:Array<Int>, z:Array<UInt>) {}
}


abstract TestColor(Int) from Int from UInt to Int to UInt {}
package;
class InheritUnspecifiedTypeParameters {

    function createObject<A, B:{length:Int}>(value1:A, ?value2:B):TestClass<A, B> {}

    function testTypeParameters(test:TestClass<String, Array<String>>) {}

    public function new() {
        // Correct, we dont have anything defining typeParameter B, so we get whatever is expected.
        testTypeParameters(createObject("onlyOneArgument")); // correct

        // Correct,  all typeParameters from argument matches expected
        testTypeParameters(createObject("onlyOneArgument", ["1", "2"]));

        // Wrong,  parameters typeParameter expects array of String,
        testTypeParameters(<error descr="Type mismatch (Expected: 'TestClass<String, Array<String>>' got: 'TestClass<String, Array<Int>>')">createObject("onlyOneArgument", [1, 2])</error>);
    }
}

class TestClass<T, Q:{length:Int}> {}
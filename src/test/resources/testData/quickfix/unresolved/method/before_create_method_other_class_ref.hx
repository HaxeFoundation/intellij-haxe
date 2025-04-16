// "Create method 'testMethod'" "true-preview"
class OtherClass {}
class Test {
    function test() {
        var ref:OtherClass = null;
        ref.testMethod<caret>();
    }
}

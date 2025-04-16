// "Create method 'testMethod'" "true-preview"
class OtherClass {}
class Test {
    function test() {
        OtherClass.testMethod<caret>(();
    }
}
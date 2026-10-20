// "Create local function 'getVal'" "true-preview"
class Test {
    function test() {
        function getVal(myStrParam:String, i:Int, f:Float):Dynamic {
            return null;
        }

        var x = getVal("myStrParam", 10, 2.0);
    }
}
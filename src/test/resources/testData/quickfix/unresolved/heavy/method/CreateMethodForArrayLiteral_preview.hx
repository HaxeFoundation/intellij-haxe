// "Create method 'unresolvedMethod'" "true-preview"
class Test {
    function test() {
        var a:Array<Int -> Void> = [unresolvedMethod];
    }

    private function unresolvedMethod(p0:Int):Void {

    }
}
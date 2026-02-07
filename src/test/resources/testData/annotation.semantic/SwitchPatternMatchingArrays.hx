class PatternMachingTest {

    public function test1DArray() {
        var myArray = [1, 2, 3];
        var s = switch (myArray) {
            case [a, b, c]: a + b + c;
            case a: 0;
        }
        trace(s);
    }

    public function test2DArray() {
        var myArray = [[1, 2, 3], [4, 5, 6]];
        var s = switch (myArray) {
            case [[a, b, _],[_,c,_]]: a + b + c;
            case a: 0;
        }
        trace(s);
    }
    public function testArrayInObject() {
        var myObjectWithArray = {arr:[[1,2], [3]]};
        var s = switch (myObjectWithArray) {
            case {arr:[[a, b], [c]] }: a + b + c;
            case a: 0;
        }
    }

}
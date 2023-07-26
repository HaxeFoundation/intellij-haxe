package ;
class TestSwitchCase {
    public function new() {
        var correctFromSwitchA:Int = recursiveSwitchA(1);
        var correctFromSwitchB:Int = recursiveSwitchB(1);
        var correctFromIf:String = recursiveIf(1);

        var <error descr="Incompatible type: Int should be TestSwitchCase">wrongFromSwitchA:TestSwitchCase = recursiveSwitchA(1)</error>; // expected type missmatch
        var <error descr="Incompatible type: Int should be TestSwitchCase">wrongFromSwitchB:TestSwitchCase = recursiveSwitchB(1)</error>; // expected type missmatch
        var <error descr="Incompatible type: String should be TestSwitchCase">wrongFromIf:TestSwitchCase = recursiveIf(1)</error>;  // should give type missmatch

    }

    // should resolve to String
    public function recursiveIf(i:Int) {
        if (i == 1) {
            return recursiveIf(1 + 1);
        }else {
            return "";
        }
    }
    // should resolve to Int
    public function recursiveSwitchA( i:Int) {
        return switch(i) {
            case 10 : recursiveSwitchA(i -1);
            case 9: 1;
        }
    }
    // should resolve to Int
    public function recursiveSwitchB( i:Int) {
        switch(i) {
            case 10 : return recursiveSwitchB(i -1);
            case 9: return 1;
        }
    }
}

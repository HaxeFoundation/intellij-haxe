package ;
class TestSwitchCase {
    public function new() {
        var correctFromSwitchA:Int = recursiveSwitchA(1);
        var correctFromSwitchB:Int = recursiveSwitchB(1);
        var correctFromIf:String = recursiveIf(1);

        var wrongFromSwitchA:TestSwitchCase = <error descr="Incompatible type: Int should be TestSwitchCase">recursiveSwitchA(1)</error>; // expected type missmatch
        var wrongFromSwitchB:TestSwitchCase = <error descr="Incompatible type: Int should be TestSwitchCase">recursiveSwitchB(1)</error>; // expected type missmatch
        var wrongFromIf:TestSwitchCase = <error descr="Incompatible type: String should be TestSwitchCase">recursiveIf(1)</error>;  // should give type missmatch

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

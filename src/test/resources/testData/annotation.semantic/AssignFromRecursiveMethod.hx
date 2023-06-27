package ;
class TestSwitchCase {
    public function new() {
        var correctFromSwitch:Int = recursiveSwitch(1);
        var correctFromIf:String = recursiveIf(1);

        var <error descr="Incompatible type: Int should be TestSwitchCase">wrongFromSwitch:TestSwitchCase = recursiveSwitch(1)</error>; // expected type missmatch
        //TODO mlo
//        var wrongFromIf:TestSwitchCase = recursiveIf(1);  // should give type missmatch  (but currently not implemented)

    }

    // should resolve to Int
    public function recursiveIf( i:Int) {
        if (i == 1) {
            return recursiveSwitch(1 + 1);
        }else {
            return "";
        }
    }
    // should resolve to String
    public function recursiveSwitch( i:Int) {
        return switch(i) {
            case 10 : return recursiveSwitch(i -1);
            case 9:  return 1 ;
        }
    }
}

package ;
interface Level1Interface {
    function level1Method(level1Param:Bool):Void;
}
interface Level2Interface extends Level1Interface {
    function level2Method():Void;
}
interface Level3Interface extends Level2Interface {
    function level3Method():Void;
}

class WrongLevel1Signature implements Level3Interface {
    public function new() {}
    public function level3Method():Void {}
    public function level2Method():Void {}
    public function level1Method(<error descr="Incompatible type: String should be Bool">level1Param:String</error>):Void {}
}

interface GenericLevel1Interface<T> {
    function level1Get():T;
}
interface GenericLevel2Interface<T> extends GenericLevel1Interface<T> {
    function level2Set(level2Value:T):Void;
}
interface StringLevel3Interface extends GenericLevel2Interface<String> {
    function level3Name():String;
}
class ImplementsGenericLevels implements StringLevel3Interface {
    public function new() {}
    public function level1Get():String {return null;}
    public function level2Set(level2Value:String):Void {}
    public function level3Name():String {return null;}
}

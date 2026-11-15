package ;
interface Level1Interface {
    var level1Field:Bool;
}
interface Level2Interface extends Level1Interface {
    var level2Field:String;
}
interface Level3Interface extends Level2Interface {
    var level3Field:Int;
}

class MissesLevel1Field implements <error descr="Not implemented fields: level1Field">Level3Interface</error> {
    public var level3Field:Int;
    public var level2Field:String;
    public function new() {}
}
class MissesLevel2Field implements <error descr="Not implemented fields: level2Field">Level3Interface</error> {
    public var level3Field:Int;
    public var level1Field:Bool;
    public function new() {}
}
class ImplementsAllFieldLevels implements Level3Interface {
    public var level3Field:Int;
    public var level2Field:String;
    public var level1Field:Bool;
    public function new() {}
}

class WrongLevel1FieldType implements Level3Interface {
    public var level3Field:Int;
    public var level2Field:String;
    <error descr="Field level1Field has different type than in  Level3Interface">public var level1Field:String;</error>
    public function new() {}
}

package ;
interface Level1Interface {
    function level1Method():Void;
}
interface Level2Interface extends Level1Interface {
    function level2Method():Void;
}
interface Level3Interface extends Level2Interface {
    function level3Method():Void;
}

class MissesLevel1Method implements <error descr="Not implemented methods: level1Method">Level3Interface</error> {
    public function new() {}
    public function level3Method():Void {}
    public function level2Method():Void {}
}
class MissesLevel2Method implements <error descr="Not implemented methods: level2Method">Level3Interface</error> {
    public function new() {}
    public function level3Method():Void {}
    public function level1Method():Void {}
}
class MissesLevel2AndLevel1Methods implements <error descr="Not implemented methods: level2Method, level1Method">Level3Interface</error> {
    public function new() {}
    public function level3Method():Void {}
}
class ImplementsAllLevels implements Level3Interface {
    public function new() {}
    public function level3Method():Void {}
    public function level2Method():Void {}
    public function level1Method():Void {}
}

class Level1BaseClass {
    public function new() {}
    public function level1Method():Void {}
}
class InheritsLevel1MethodFromBaseClass extends Level1BaseClass implements Level3Interface {
    public function new() {super();}
    public function level3Method():Void {}
    public function level2Method():Void {}
}

class ImplementsLevel2MissesLevel1Method implements <error descr="Not implemented methods: level1Method">Level2Interface</error> {
    public function new() {}
    public function level2Method():Void {}
}

interface Level2RedeclaresLevel1 extends Level1Interface {
    function level1Method():Void;
}
class MissesRedeclaredLevel1Method implements <error descr="Not implemented methods: level1Method">Level2RedeclaresLevel1</error> {
    public function new() {}
}

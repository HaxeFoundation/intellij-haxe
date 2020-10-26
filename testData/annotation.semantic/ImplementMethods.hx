package ;
interface TestInterface {
    function MethodA(i:Int):Void;
    function MethodB(i:Int):Void;
}

class BaseClassPartial {
    public function MethodA(i:Int):Void {}
    public function new() {}
}
class BaseClassComplete {
    public function MethodA(i:Int):Void {}
    public function MethodB(i:Int):Void {}
    public function new() {}
}

class BaseCoversMethodA extends  BaseClassPartial implements TestInterface {
    public function MethodB(i:Int):Void {}
    public function new() {}
}

class BaseCoversBothMethods extends  BaseClassComplete implements TestInterface {
    public function new() {}
}

class MissingMethods implements <error descr="Not implemented methods: MethodA, MethodB">TestInterface</error> {
    public function new() {}
}
class BaseCoversMethodAButNotB extends BaseClassPartial implements <error descr="Not implemented methods: MethodB">TestInterface</error> {
    public function new() {}
}
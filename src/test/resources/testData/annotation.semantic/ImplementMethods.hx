package ;
interface TestInterface {
    function MethodA(i:Int):Void;
    function MethodB(i:Int):Void;
    function MethodC(i:Int):TestInterface;
}

class BaseClassPartial {
    public function new() {}
    public function MethodA(i:Int):Void {}
    public function MethodC(i:Int):TestInterface {return null;}
}
class BaseClassComplete {
    public function new() {}
    public function MethodA(i:Int):Void {}
    public function MethodB(i:Int):Void {}
    public function MethodD(i:Int):BaseClassPartial {return null;}
}

class BaseCoversMethodA extends  BaseClassPartial implements TestInterface {
    public function new() {super();}
    public function MethodB(i:Int):Void {}
    override public function MethodC(i:Int):BaseCoversMethodA {return null;}
}

class BaseCoversBothMethods extends  BaseClassComplete implements TestInterface {
    public function new() {super();}
    public function MethodC(i:Int):BaseCoversBothMethods {return null;}
    override public function MethodD(i:Int):BaseCoversMethodA {return null;}
}

class MissingMethods implements <error descr="Not implemented methods: MethodA, MethodB, MethodC">TestInterface</error> {
    public function new() {}
}
class BaseCoversMethodAButNotB extends BaseClassPartial implements <error descr="Not implemented methods: MethodB">TestInterface</error> {
    public function new() {super();}
}

class WrongMethodsA implements TestInterface {
    public function new() {}
    public function MethodA(<error descr="Incompatible type: String should be Int">i:String</error>):Void {} // WRONG parameter
    public function MethodB(i:Int)<error descr="Incompatible return type: String should be Void">:String</error> {return null;} // WRONG return type
    public function MethodC(<error descr="Incompatible type: String should be Int">i:String</error>)<error descr="Incompatible return type: String should be TestInterface">:String</error> {return null;} // WRONG parameter and return type
}
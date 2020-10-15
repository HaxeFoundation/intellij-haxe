package;

class FinalInInterface implements TestInterface {

    public final field:Int = 1;
    public function test() {}
    public function new() {}

}

interface TestInterface {
    final field:Int;
    final function test():Void;
}
package;

class FinalInInterface implements <error descr="Not implemented fields: fieldWrong">TestInterface</error> {

    public final field:Int = 1;
    public function test() {}
    public function new() {}

}

interface TestInterface {
    final field:Int;
    <error descr="Default values on interfaces are not allowed" textAttributesKey="ERRORS_ATTRIBUTES">final fieldWrong:Int = 1;</error>
    final function test():Void;
}
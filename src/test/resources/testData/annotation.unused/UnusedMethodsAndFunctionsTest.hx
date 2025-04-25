class UnusedMethodsAndFunctionsTest extends BaseClass {
    public function new() {
        function usedLocalFunction() {}

        usedLocalFunction();
        usedStaticMethod();
        usedPrivateMethod();
        usedDefaultMethod();

        function <warning descr="Function 'unusedLocalFunction' is never used">unusedLocalFunction</warning>() {}
    }

    private static function usedStaticMethod() {}
    private function usedPrivateMethod() {}
    function usedDefaultMethod() {}

    private static function outsideUsageStaticMethod() {}

    private static function <warning descr="Method 'unusedStaticMethod' is never used">unusedStaticMethod</warning>() {}
    private function <warning descr="Method 'unusedPrivateMethod' is never used">unusedPrivateMethod</warning>() {}
    function <warning descr="Method 'unusedDefaultMethod' is never used">unusedDefaultMethod</warning>() {}


    //  ignore  overrides even if private
    override function functionToOverride() {}

}
interface InterfaceTest {
    function interfaceMethodsUnaffected():Void;
}

class BaseClass {
    function <warning descr="Method 'functionToOverride' is never used">functionToOverride</warning>() {}
}
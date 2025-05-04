package;
class ReturnStatementAnnotation {
    public function new() {
    }

    public function correctA():Int {
        return 1;
    }
    public function correctB():Int->String {
        return String.fromCharCode;
    }

    public function correctC():Void {
        return;
    }


    public function missingStatment():Int {
        <error descr="Missing return statement">}</error>

    public function missingValue():Int {
        <error descr="Incompatible type: Void should be Int">return;</error>
    }

    public function wrongTypeA():Int {
        return <error descr="Incompatible type: String should be Int">"str"</error>;
    }

    public function wrongTypeB():String->Int {
        return <error descr="Incompatible type: Int->String should be String->Int">String.fromCharCode</error>;
    }

    public function noValueExprectedA():Void {
        return <error descr="Cannot return 'str' from Void-function">"str"</error>;
    }

    public function noValueExprectedB():Void {
        return <error descr="Cannot return 'null' from Void-function">null</error>;
    }

    public function nullWarning():Int {
        return <weak_warning descr="On static platforms, null can't be used as basic type Int">null</weak_warning>;
    }
}

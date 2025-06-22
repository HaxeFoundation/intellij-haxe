using extensions.FunctionExtensions;

class Test {
    static function main() {
        var fn:Int->Int = (i)->i*2;
        // correct
        var x = fn.testExtension( 1);
        // wrong
        var x = fn.<warning descr="Unresolved symbol">NonExsistingExtension</warning>(1);
    }
}

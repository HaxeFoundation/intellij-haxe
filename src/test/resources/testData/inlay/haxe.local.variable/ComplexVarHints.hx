package testData.inlay.haxe.local.variable;
class ComplexVarHints {
    public function new() {

        // Verify correct inlay for normal construcot with typeParameters
        var normalConstructorValue/*<# :Map<String, Int> #>*/  = new Map<String, Int>();

        // verify correct inlay for constructor where arguments provides the typeParameters
        var argumentConstructorValue/*<# :ConstTest<String, Int> #>*/ = new ConstTest(normalConstructorValue);

        // verify correct inlay when typedef is used and arguements provide typeParameter
        var typedefVaue/*<# :TypedefTest<Int> #>*/ = new TypedefTest(normalConstructorValue);

    }
}

typedef  TypedefTest<V> = ConstTest<String, V>

abstract ConstTest<K, V>(Map<K, V>) {
    function new (m:Map<K,V>) {
        this = m;
    }
}
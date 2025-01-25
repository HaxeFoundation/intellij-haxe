class ParameterMonomorph {


    public function assignFieldHint()/*<# :Void #>*/ {
        var array/*<# :Array<String> #>*/  = new Array();
        var myArrayHolder:MyTestClass<Array<String>>= new MyTestClass(array);
    }

    public function assignReferenceHint()/*<# :Void #>*/ {
        var myArrayHolder:MyTestClass<Array<String>>;
        var array/*<# :Array<String> #>*/ = new Array();
        myArrayHolder = new MyTestClass(array);
    }


}
class MyTestClass<T> {
    var x = x;
    function new (x:T) {
        this.x = x;
    }
}


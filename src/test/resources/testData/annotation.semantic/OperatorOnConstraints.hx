class OperatorOnConstraintsString<T:String> {
    var x:T;
    var y:T;
    public function test() {
        var Ok = x + y; // allowed, result is String
        var Wrong = <error descr="Unable to apply operator * for types T:String and T:String">x * y</error>; // wrong cant multiply types of string
    }
}

class OperatorOnConstraintsInt<T:Int> {
    var x:T;
    var y:T;
    public function test() {
        var Ok = x * y;
    }
}

// NO_HINTS
class Test {

    static function use<T>(arr:Array<T>) {
        keep(arr);
    }

    // The call site's type Array<T> names the CALLER's type parameter. The
    // compiler would freeze it into this signature (first unification wins),
    // making every other call site a type error - the documented
    // order-dependent over-specialization. A hint would show a T that means
    // nothing in this scope and drive false mismatch errors, so the binding
    // is rejected and the parameter stays open. The proper generalization
    // (treat keep as implicitly generic) is future work.
    static function keep(value) {
    }
}
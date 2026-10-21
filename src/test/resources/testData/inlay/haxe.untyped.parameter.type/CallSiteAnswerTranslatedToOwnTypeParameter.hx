class Test {

    // sort's cmp is declared T->T->Int in sort's OWN type parameter; at the
    // rec call site rec.T is bound to sort.T through the array argument, so
    // the answer translates into rec's vocabulary instead of being rejected
    // as a foreign type parameter
    static public function sort<T>(a:Array<T>, cmp:T->T->Int):Void {
        rec(a, cmp, 0, a.length);
    }

    static function rec<T>(a:Array<T>, cmp/*<# :|(|T|, |T|)|->|Int #>*/, from:Int, to:Int):Void {
    }
}

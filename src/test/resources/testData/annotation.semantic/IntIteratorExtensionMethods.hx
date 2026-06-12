using extensions.IntIteratorExtensions;

class Test {
    static function main() {
        var letters = ["a", "b", "c"];

        // a `min...max` range is an IntIterator, so extension methods on IntIterator must apply
        for (i in (0...letters.length).reverse()) {
            var s:String = letters[i];
        }

        // extension methods declared against Iterator<Int> apply too (IntIterator unifies structurally)
        var total:Int = (0...letters.length).sum();

        // IntIterator instance members resolve on range expressions
        var range = 0...letters.length;
        var more:Bool = range.hasNext();

        // ranges can be assigned to IntIterator as well as Iterator<Int>
        var typed:IntIterator = 0...10;
        var structural:Iterator<Int> = 0...10;

        // plain range loops still infer Int elements
        for (i in 0...letters.length) {
            var ok:Int = i;
        }

        // unknown members are still reported
        (0...10).<warning descr="Unresolved symbol">notARealExtension</warning>();
    }
}

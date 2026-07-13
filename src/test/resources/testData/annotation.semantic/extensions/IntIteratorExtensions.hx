package extensions;

class IntIteratorExtensions {
    public static function reverse(it:IntIterator):Array<Int> {
        var result = [for (i in it) i];
        result.reverse();
        return result;
    }

    public static function sum(it:Iterator<Int>):Int {
        var total = 0;
        for (i in it) total += i;
        return total;
    }
}

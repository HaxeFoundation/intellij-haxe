package;
class MapLiteralComprehensionForWhile {
    public function MapLiteralComprehensionForWhile() {
        var words = ["one", "two", "three"];
        var i = 0;
        var map = [
            for (j in 0...words.length)
                while (i < words.length)
                    i + j => words[i%j]
        ];
    }
}
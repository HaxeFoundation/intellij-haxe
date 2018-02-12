package;
class MapLiteralComprehensionWhileFor {
    public function MapLiteralComprehensionWhileFor() {
        var words = ["one", "two", "three"];
        var i = 0;
        var map = [
            while (i < words.length)
                for (j in 0...words.length)
                    i + j => words[i % j]
        ];
    }
}
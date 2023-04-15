package;
class MapLiteralComprehensionForFor {
    public function MapLiteralComprehensionForFor() {
        var words = ["one", "two", "three"];
        var map = [
            for (n in 0...words.length)
                for (m in 0...words.length)
                    n + m => words[n] + words[m]
        ];
    }
}
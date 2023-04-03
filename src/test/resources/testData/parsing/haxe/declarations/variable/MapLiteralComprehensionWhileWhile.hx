package;
class MapLiteralComprehensionWhileWhile {
    public function MapLiteralComprehensionWhileWhile() {
        var words = ["one", "two", "three"];
        var i = 0;
        var j = 0;
        var map = [
            while (i < words.length)
                while (j < words.length * i)
                    i + j => i++ * j++
        ];
    }
}
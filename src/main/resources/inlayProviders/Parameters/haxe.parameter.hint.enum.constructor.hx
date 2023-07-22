enum Color {
    Red;
    Green;
    Blue;
    Rgb(r:Int, g:Int, b:Int);
}

class Test {
    static function main() {
        var redColor = Color.Red;
        var rgb = Color.Rgb(1, 0, 1);
    }

}

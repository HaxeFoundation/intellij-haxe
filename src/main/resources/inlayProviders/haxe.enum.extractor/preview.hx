enum Color {
    Red;
    Green;
    Blue;
    Rgb(r:Int, g:Int, b:Int);
}

class Test {
    static function main() {
        var rgb = Color.Rgb(1, 0, 1);
        switch(rgb) {
            case Rgb(r<# : Int #> , g<# : Int #>, b<# : Int #>) :
                trace("R:" + r + " G " + g + " B " + b);
        }
    }

}

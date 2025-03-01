class Test {

    public static function main() {
        filter(function(arg/*<# :|String #>*/) {return true;});
    }

    public function filter(fn:String -> Bool) {
    }
}
class Test {
    static function fromVariable() {
        var array = ["A", "B", "C"];
        for (myChar/*<# :String #>*/ in array) {
            trace("myChar");
        }

        // verify that we can find iterator when wrapped in null
        var nullTest:Null<Array<String>> = new Array();
        for (nullMyChar/*<# :String #>*/ in nullTest) {
            trace("myChar");
        }
    }

    static function fromParameter0(arr:Iterable<Dynamic>){
        for (val/*<# :Dynamic #>*/ in arr) {
            var x = val;
        }
    }
    static function fromParameter1<T>(arr:Iterable<T>){
        for (val/*<# :T #>*/ in arr) {
            var x = val;
        }
    }
    static function fromParameter2(arr:Array<Iterable<String>>){

        for (val/*<# :Iterable<String> #>*/ in arr) {
            var x = val;
        }

        for (val/*<# :String #>*/ in arr[0]) {
            var x = val;
        }
    }
}
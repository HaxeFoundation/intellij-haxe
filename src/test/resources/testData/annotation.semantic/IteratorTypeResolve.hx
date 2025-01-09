package;

import haxe.ds.IntMap;

class Test {

  public static function main() {

      // iterate map
        var map = [ 1 => "one", 2 => "two" ];
        var intmap:IntMap<String> = [ 1 => "one", 2 => "two" ];
        var mapi:Map<Int, String> = [ 1 => "one", 2 => "two" ];

        for (key => value in map) {
           value.length * key;
        }
        for (key => value in intmap) {
            value.length * key;
        }
        for (key => value in mapi) {
            value.length * key;
        }

      // iterate Array
        var arr1:Array<String>;
        var arr2 = new Array<String>();

        arr1.iterator();

        for (value in arr1) {
          value.length;
        }
        for (value in arr2) {
          value.length;
        }

        for (key => value in arr1) {
          value.length * key;
        }
        for (key => value in arr2) {
          value.length * key;
        }
        // iterate string
        var str:String;

        //strings can be iterated if we have using StringTools  (extension method)

        for (value in str) {
            // value is character code
            var ok:Int = value;
            var wrong:String = value;
        }

        for (key => value in str) {
            var ok:Int = value * key;
            // both key and value are Ints (ofset and character code)
            var wrongKey:String = key;
            var wrongValue:String = value;
        }

  }
}
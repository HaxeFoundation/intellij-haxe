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
            //TODO mlo: fix test standard lib so this works (added after 4.0.5)
          value.<warning descr="Unresolved symbol">length</warning> * key;
        }
        for (key => value in arr2) {
            //TODO mlo: fix test standard lib so this works (added after 4.0.5)
          value.<warning descr="Unresolved symbol">length</warning> * key;
        }
        // iterate string
        var str:String;
        for (value in str) {
            //TODO mlo: fix test standard lib so this works
          value.<warning descr="Unresolved symbol">length</warning>;
        }

        for (key => value in str) {
          value.length * key;
        }

  }
}
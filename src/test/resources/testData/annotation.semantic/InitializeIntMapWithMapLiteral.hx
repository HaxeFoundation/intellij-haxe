package;

import haxe.ds.IntMap;

class Test {
  private var map = [ 1 => "one", 2 => "two" ];
  private var intmap:IntMap<String> = [ 1 => "one", 2 => "two" ];
  private var mapi:Map<Int, String> = [ 1 => "one", 2 => "two" ];

  public static function main() {
    var t = new Test();

    t.map = t.intmap;
    t.mapi = t.intmap;
    t.intmap = mapi;
    t.intmap = map;
  }
}
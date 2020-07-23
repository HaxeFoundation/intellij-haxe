package;

import haxe.ds.EnumValueMap;

enum MyEnum {
  FIRST;
  SECOND;
}

class Test {
  private var map = [ FIRST => "first", SECOND => "second" ];
  private var enummap:EnumValueMap<MyEnum, String> = [ FIRST => "first", SECOND => "second" ];
  private var mapi:Map<MyEnum, String> = [ FIRST => "first", SECOND => "second" ];

  public function main() {
    var t = new Test();
    var m : Map<Enum<MyEnum>, String> = t.map;
    m = t.enummap;
    m = t.mapi;

    t.map = t.enummap;  // Error in Haxe 3.4.7
    t.enummap = t.map;
    t.mapi = t.enummap; // Error in Haxe 3.4.7
    t.enummap = t.mapi;
    t.map = t.mapi;
    t.mapi = t.map;
  }
}

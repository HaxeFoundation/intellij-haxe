// Phantom-typed id abstract with an implicit cast to String, so a plain
// String array-access overload would also accept it.
abstract LibId<T>(String) to String {
  public inline function new(s:String) {
    this = s;
  }
}

class ImageLib {
  public function new() {}
  public function getImage(name:String):Int return 0;
}

// Mirrors LegendsAssets: an abstract over the base lib, self-typed, with the
// id declared with NO type annotation, inferred from new LibId<Assets>(...).
abstract Assets(ImageLib) from ImageLib to ImageLib {
  public inline function legends_prison_cagebackground():Int return this.getImage("x");
  public static inline final ID = new LibId<Assets>("assets");
}

// Two overloaded @:op([]) getters. Indexing with a LibId<T> must pick getTyped
// (returns T inferred from the argument), not get (returns ImageLib).
abstract Libs(Map<String, ImageLib>) {
  public inline function new(m:Map<String, ImageLib>) {
    this = m;
  }

  @:op([]) inline function getTyped<T>(id:LibId<T>):T {
    return cast get(id);
  }

  @:op([]) inline function get(id:String):ImageLib {
    return this[id];
  }
}

class Test {
  public function new() {
    var libs:Libs = new Libs(new Map<String, ImageLib>());

    // typed overload: index is LibId<Assets> -> result must be Assets
    var typed/*<# :|Assets #>*/ = libs[Assets.ID];

    // string overload: plain String index -> result is ImageLib
    var viaString/*<# :|ImageLib #>*/ = libs["plain"];
  }
}

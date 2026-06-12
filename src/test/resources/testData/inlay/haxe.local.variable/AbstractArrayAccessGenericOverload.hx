// Phantom-typed key abstract with an implicit cast to String, so a plain
// String array-access overload would also accept it.
abstract TypedKey<T>(String) to String {
  public inline function new(s:String) {
    this = s;
  }
}

class PictureSet {
  public function new() {}
  public function getPicture(name:String):Int return 0;
}

// An abstract over the base set, self-typed, with the key declared with NO type
// annotation, inferred from new TypedKey<Icons>(...).
abstract Icons(PictureSet) from PictureSet to PictureSet {
  public inline function starIcon():Int return this.getPicture("x");
  public static inline final KEY = new TypedKey<Icons>("icons");
}

// Two overloaded @:op([]) getters. Indexing with a TypedKey<T> must pick getTyped
// (returns T inferred from the argument), not get (returns PictureSet).
abstract Sets(Map<String, PictureSet>) {
  public inline function new(m:Map<String, PictureSet>) {
    this = m;
  }

  @:op([]) inline function getTyped<T>(id:TypedKey<T>):T {
    return cast get(id);
  }

  @:op([]) inline function get(id:String):PictureSet {
    return this[id];
  }
}

class Test {
  public function new() {
    var sets:Sets = new Sets(new Map<String, PictureSet>());

    // typed overload: index is TypedKey<Icons> -> result must be Icons
    var typed/*<# :|Icons #>*/ = sets[Icons.KEY];

    // string overload: plain String index -> result is PictureSet
    var viaString/*<# :|PictureSet #>*/ = sets["plain"];
  }
}

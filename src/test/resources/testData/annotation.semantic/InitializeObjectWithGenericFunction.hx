package;

class DisplayObject {
  public function new() {}
}

class Sprite extends DisplayObject {
  public function new() { super(); }
}

class Test {
  public static function type<T>(o:Any, t:Class<T>):T {
    return (Std.is(o,t) ? o : null);
  }

  public static function main() {
    var sprite:Sprite = new Sprite();
    var dObj:DisplayObject = type(sprite, DisplayObject); // Incompatible Type (T should be DisplayObject)
  }
}
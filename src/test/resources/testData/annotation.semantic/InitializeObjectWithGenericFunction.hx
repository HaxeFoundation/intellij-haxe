package;

class DisplayObject {
  public function new() {}
}

class Sprite extends DisplayObject {
  public function new() { super(); }
}

class Test {
  public static function type<T>(o:Any, t:Class<T>):T {
    return (<warning descr="Std.is is deprecated since Haxe 4.1; use Std.isOfType">Std.is</warning>(o,t) ? o : null);
  }

  public static function main() {
    var sprite:Sprite = new Sprite();
    var dObj:DisplayObject = type(sprite, DisplayObject); // Incompatible Type (T should be DisplayObject)
  }
}
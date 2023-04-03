package;
import haxe.Constraints.Function;

class Test {
  static function main() {
    callback = new Function();  // Generic function unifies.
    callback = cast((a,b)->0, Function); // Would be a bug, but legal Haxe.
  }
}
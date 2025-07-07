package;
import haxe.Constraints.Function;

class Test {
  static function main() {
    callback = <error descr="Function does not have a constructor">new Function()</error>;  // Generic function unifies.
    callback = cast((a,b)->0, Function); // Would be a bug, but legal Haxe.
  }
}
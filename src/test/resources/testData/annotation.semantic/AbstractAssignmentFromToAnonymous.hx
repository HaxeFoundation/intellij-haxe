package;

class AbstractAssignmentFromToAnonymous {
  static function main() {
    var a:A = new Test();
    var b: { function a():Void; } = a;
  }

  public function new() {}
  public function a() {}
}

abstract A({ function a():Void; })
from { function a():Void; }
to { function a():Void; } {}
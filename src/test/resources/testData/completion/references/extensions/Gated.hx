package extensions;

class Gate {
  public function new() {}
}

class Tools {
  @:allow(completion.references.Extensions6)
  static function allowedOperation(gate:Gate):Void {}
  static function hiddenOperation(gate:Gate):Void {}
}

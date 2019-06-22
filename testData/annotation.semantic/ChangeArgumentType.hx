class ChangeArgumentType {
  static public function test(<error descr="Incompatible type: Bool should be Int">a:Int = <caret>false</error>) {

  }
}
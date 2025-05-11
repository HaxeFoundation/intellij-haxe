class ChangeArgumentType {
  static public function test(a:Int = <error descr="Incompatible type: Bool should be Int"><caret>false</error>) {
  }
}
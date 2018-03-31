class ChangeArgumentType {
  static public function test(<error descr="Incompatible type Int can't be assigned from Bool = false">a:Int =<caret> false</error>) {

  }
}
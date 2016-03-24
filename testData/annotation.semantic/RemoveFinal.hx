package ;

class RemoveFinal extends Base {
  override public function <error descr="Can't override static, inline or final methods">te<caret>st</error>() {
  }
}

class Base {
  @:final public function test() {
  }
}
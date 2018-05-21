class Properties1 {
  var x(get, set):Int;

  private function get_x():Int {
    return 10;
  }

  private function <caret>set_x(val:Int):Int {
    return val;
  }

  public function new() {
   this.x = 10;
  }
}
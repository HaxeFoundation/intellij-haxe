class Properties1 {
  var <caret>x(get, never):Int;

  private function get_x():Int {
    return 10;
  }

  public function new() {
    trace(get_x());
  }
}
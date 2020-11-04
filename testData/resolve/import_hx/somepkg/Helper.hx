package somepkg;

class Helper {
  var stack : List<Int> = new List<Int>();  // List brought in by import.hx in this directory.

  public function new() {}
  public function push(i:Int) {
    stack.push(i);
  }
  public function pop() {
    return stack.pop();
  }
  public function length() {
    return stack.length;
  }
  public function callme(i : Imports) {  // Imports brought in by import.hx at root level.
    i.answer();
  }
  public static function show(s : String) {
    trace(s);
  }
}
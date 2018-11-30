package;

@:forward()
abstract MyArray<T>(Array<T>) from Array<T> to Array<T> {
}

class Test {
  public function new() {
    var myArray:MyArray<String> = ["Hello"];
    myArray.<caret>
  }
}
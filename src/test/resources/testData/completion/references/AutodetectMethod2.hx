class Methods {
  public function fooInt() return [1,2,3];
  public function fooString() return ['a', 'b', 'c'];
  public function fooEmpty() return [];
}

class Main {
  public static function main() {
    new Methods().<caret>
  }
}
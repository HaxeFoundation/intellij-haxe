class Test {
  public function demo(func:String -> Void):Void {
  }

  public function main() {
    var out = 10;

    this.demo(<error descr="Can't assign Int -> Int to String -> Void">function(v) {
      out = v;
    }</error>);
  }
}

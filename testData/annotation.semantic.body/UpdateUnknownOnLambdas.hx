class Test {
  public function demo(func:String -> Void):Void {
  }

  public function main() {
    var out;

    this.demo(function(v) {
      out = v;
    });

    <error descr="Can't assign Int to String">out = 10</error>;
  }
}

class Test {
  public function demo(func:String -> Void):Void {
  }

  public function main() {
    var out = 10;

    this.demo(function(v) {
      <error descr="Can't assign String to Int">out = v</error>;
    });
  }
}

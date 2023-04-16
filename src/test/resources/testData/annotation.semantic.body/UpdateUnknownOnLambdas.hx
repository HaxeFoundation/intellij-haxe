class Test {
  public function demo(func:String -> Void):Void {
  }

  public function main() {
    var out = 10;

    this.demo(function(v) {
      out = <error descr="Incompatible type: String should be Int">v</error>;
    });
  }
}

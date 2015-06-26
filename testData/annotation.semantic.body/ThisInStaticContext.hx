class Test {
  public function demo() {
    this;

    function lambda() {
      this.demo();
    }

    var test = function() {
      this.demo();
    };
  }

  static public function demo2() {
    this.demo();
    function lambda() {
      this.demo();
    }

    var test = function() {
      this.demo();
    };
  }
}
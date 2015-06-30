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
    <error descr="Using this in a static context">this</error>.demo();
    function lambda() {
      <error descr="Using this in a static context">this</error>.demo();
    }

    var test = function() {
      <error descr="Using this in a static context">this</error>.demo();
    };
  }
}
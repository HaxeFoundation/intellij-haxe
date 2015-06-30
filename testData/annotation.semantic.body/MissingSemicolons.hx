class Test {
  public function demo() {
    var a = <error descr="Missing semicolon">3</error>
    a = 1<error descr="Missing semicolon">0</error>
    if (a != 10) {
    a = <error descr="Missing semicolon">4</error>
    } else {
    a = <error descr="Missing semicolon">5</error>
    }
    while (a != 7) {
    a = <error descr="Missing semicolon">8</error>
    }
    do {
    a = 9;
    } while(<error descr="While expression must be Bool but was Int = 10">a = 10</error>);
  }

  public function demo2() return 1<error descr="Missing semicolon"><error descr="Missing semicolon">0</error></error>
}
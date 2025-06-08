enum TestNum {
  valueA;
  valueB;
}
class ReturnStatementMissingAnnotatorTest {

  //
  // CORRECT
  //

  public function ok_return():Int {
    return 1;
  }

  public function ok_throw():Int {
    throw 1;
  }

  public function ok_if():Int {
    if (true) {/*Nothing*/}
    return 1;
  }

  public function ok_ifElse():Int {
    if (true) {
      return 1 ;
    } else {
      return 1;
    }
  }

  public function ok_switch():Int {
    switch ("x") {
      case "A": return 1;
      default: return 1;
    }
  }

  public function ok_switch_enum():Int {
    var x:TestNum = valueB;
    switch (x) {
      case valueA: return 1;
      case valueB: return 1;
    }
  }

  public function ok_try_catch():Int {
    try {
      return 1;
    } catch (x) {
      return 2 ;
    }
  }

  //
  // WRONG
  //


  public function wrong_no_return_or_throw():Int {
    // no return or throw
    <error descr="Missing return statement">}</error>


  public function wrong_if():Int {
    if (false) {
      return 1;
    }
    // no return or throw
    <error descr="Missing return statement">}</error>

  public function wrong_switch():Int {
    var x:Dynamic;
    switch (x) {
      case "A": return 1;
      case "b": return 1;
      // no default & not covering everything ("everything" require enum)
    }
    <error descr="Missing return statement">}</error>

  public function wrong_try_catchA():Int {
    try {
      return 1;
    } catch (x) {
      // no return or throw
    }
    <error descr="Missing return statement">}</error>


  public function wrong_try_catchB():Int {
    try {
      // no return or throw
    } catch (x) {
      return 1;
    }
    <error descr="Missing return statement">}</error>

  public function wrong_try_catchC():Int {
    try {
      return 1;
    } catch (x:Int) {
      // no return or throw
    } catch (y:String) {
      return 1;
    }
    <error descr="Missing return statement">}</error>

}

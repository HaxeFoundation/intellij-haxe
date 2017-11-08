package;
class TestInit {
  private static var init = {
    TestInit.FIRST = "#First!";
    TestInit.SECOND = "#Second.";
  }

  private function doit() {}  // <-- Used to show an error here.
}
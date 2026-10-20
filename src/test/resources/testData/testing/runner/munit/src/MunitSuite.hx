class MunitSuite extends massive.munit.TestSuite {
  public function new() {
    super();
    add(cases.MunitCase);
  }
}

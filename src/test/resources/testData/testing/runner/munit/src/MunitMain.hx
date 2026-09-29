import massive.munit.TestRunner;
import massive.munit.client.PrintClient;

class MunitMain {
  static function main() {
    var runner = new TestRunner(new PrintClient());
    runner.run([MunitSuite]);
  }
}

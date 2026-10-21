import tink.testrunner.Runner;
import tink.unit.TestBatch;

class TinkMain {
  static function main() {
    Runner.run(TestBatch.make([new TinkCase()])).handle(Runner.exit);
  }
}

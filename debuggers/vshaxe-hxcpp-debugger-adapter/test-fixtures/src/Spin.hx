/**
 * Long-running debuggee fixture: spins until killed, so integration tests
 * can exercise PAUSE against a genuinely running program. The loop bound is
 * a runtime value so the compiler cannot fold the loop away.
 */
class Spin {
  static function main() {
    var running = Std.parseInt("1") == 1;
    var ticks = 0;
    while (running) {
      ticks++; // bp:spin
      Sys.sleep(0.01);
    }
    trace('done ticks=$ticks'); // unreachable; keeps ticks observable
  }
}

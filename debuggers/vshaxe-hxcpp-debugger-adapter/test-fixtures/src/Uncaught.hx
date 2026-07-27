/**
 * Debuggee fixture that dies of an uncaught exception, for testing the
 * exception-stop presentation (the server reports a critical-error stop
 * with the thrown value's text). Runtime values keep the throw un-folded.
 */
class Uncaught {
  static function main() {
    var n = Std.parseInt("2");
    trace('starting n=$n');
    if (n > 1) {
      throw "kaboom " + n; // bp:throw
    }
    trace("unreachable");
  }
}

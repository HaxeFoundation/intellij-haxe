/**
 * Debuggee fixture for the HXCPP debugger integration tests.
 *
 * Compiled with -debug and -lib hxcpp-debug-server (see fixture.hxml); its
 * Server connects to the adapter on HXCPP_DEBUG_PORT and holds the program
 * before main until the debugger sends continue.
 *
 * Every loop bound and array index is a RUNTIME value (Std.parseInt) so the
 * compiler cannot unroll, fold or fuse away the code the tests set
 * breakpoints in — the same lesson as the HashLink fixtures.
 *
 * Tests find breakpoint lines by the trailing "// bp:" markers, so lines can
 * move freely as long as the markers stay.
 */
class Config {
  public var count:Int;
  public var title:String;

  public function new(count:Int, title:String) {
    this.count = count;
    this.title = title;
  }
}

class Main {
  static function main() {
    var n = Std.parseInt("3");
    var items = [for (i in 0...n) i * 10];
    var cfg = new Config(n, "fixture");
    var total = 0;
    for (i in 0...n) {
      total = accumulate(total, items[i]); // bp:loop
    }
    trace('total=$total title=${cfg.title}'); // bp:done
  }

  static function accumulate(acc:Int, v:Int):Int {
    var doubled = v * 2;
    return acc + doubled; // bp:accumulate
  }
}

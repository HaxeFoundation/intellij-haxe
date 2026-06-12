using extensions.TextTools;

class Test {
  static function main() {
    var text = "abc";

    // a bare enum constructor in extension-call argument position resolves through
    // the expected type of the matching parameter (PadSide), even though the enum's
    // module is never imported here
    var padded = text.pad(LeftSide);

    // same for enum constructors that take arguments
    var wrapped = text.wrap(Boxed("x"));

    // unknown constructors are still reported
    text.pad(<warning descr="Unresolved symbol">MiddleSide</warning>);
  }
}

class Test {

  // Declared deepest-first: the first parameter the pass evaluates sits at
  // the end of the call chain, so its inference must follow the whole chain
  // of call sites in one query - 13 hops, well past what a per-hop depth cap
  // allowed but within the probe work budget (a linear chain spends one
  // argument evaluation per hop). The compiler types every parameter Int
  // (main's literal propagates).
  static function f14(v14/*<# :|Int #>*/) {}

  static function f13(v13/*<# :|Int #>*/) {
    f14(v13);
  }

  static function f12(v12/*<# :|Int #>*/) {
    f13(v12);
  }

  static function f11(v11/*<# :|Int #>*/) {
    f12(v11);
  }

  static function f10(v10/*<# :|Int #>*/) {
    f11(v10);
  }

  static function f9(v9/*<# :|Int #>*/) {
    f10(v9);
  }

  static function f8(v8/*<# :|Int #>*/) {
    f9(v8);
  }

  static function f7(v7/*<# :|Int #>*/) {
    f8(v7);
  }

  static function f6(v6/*<# :|Int #>*/) {
    f7(v6);
  }

  static function f5(v5/*<# :|Int #>*/) {
    f6(v5);
  }

  static function f4(v4/*<# :|Int #>*/) {
    f5(v4);
  }

  static function f3(v3/*<# :|Int #>*/) {
    f4(v3);
  }

  static function f2(v2/*<# :|Int #>*/) {
    f3(v2);
  }

  static function f1(v1/*<# :|Int #>*/) {
    f2(v1);
  }

  public static function main() {
    f1(1);
  }
}

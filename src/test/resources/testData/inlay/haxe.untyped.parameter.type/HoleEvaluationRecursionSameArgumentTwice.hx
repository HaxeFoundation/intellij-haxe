class Test {

  public static function main() {
    use(1);
  }

  // w is Int from main's call site; the body passes w into BOTH slots of a
  // generic call, so hole-evaluating pair(w, w) for one slot evaluates the
  // other slot - which is w again, re-entering the same question
  static function use(w/*<# :|Int #>*/) {
    pair(w, w);
  }

  static function pair<T>(a:T, b:T):T return a;
}

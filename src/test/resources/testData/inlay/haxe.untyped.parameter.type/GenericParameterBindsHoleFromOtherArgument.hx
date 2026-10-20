class Test {

  public static function main() {}

  // no informative call site: w is typed by its body usage alone. The
  // generic call binds T to Int from the literal second argument, and w
  // lands in parameter a:T, so the compiler resolves w to Int.
  static function use(w/*<# :|Int #>*/) {
    pair(w, 1);
  }

  static function pair<T>(a:T, b:T):T return a;
}

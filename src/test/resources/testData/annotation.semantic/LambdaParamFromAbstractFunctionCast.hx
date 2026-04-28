abstract Callback<T>(T->Void) from T->Void {}

class Holder<T> {
  public function new() {}
  public function bind(cb:Callback<T>):Void {}
}

class LambdaParamFromAbstractFunctionCast {
  var _intHolder:Holder<Int> = new Holder();
  var _boolHolder:Holder<Bool> = new Holder();
  function _voidA() {}
  function _voidB() {}
  function _intToVoid(v:Int) {}

  function variantMatrix() {
    // Failing case before fix: unparenthesized `_`
    _intHolder.bind(_ -> _voidA());

    // Same code path as the failing case — confirms the fix isn't `_`-specific
    _intHolder.bind(x -> _voidA());

    // Control: explicitly typed parenthesized parameter — was never broken
    _boolHolder.bind((b:Bool) -> _voidB());

    // Control: direct method reference — no inference needed
    _intHolder.bind(_intToVoid);
  }
}

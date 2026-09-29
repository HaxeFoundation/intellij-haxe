class IsExpressionPre42Semantics {
  function f(a:Dynamic):Bool {
    return <error descr="Unparenthesized \"is\" expression cannot be used here. (pre-4.2 semantics)">a is IsExpressionPre42Semantics</error>;
  }
}

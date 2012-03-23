class LocalVariable {
  function bar(){
    var foo:LocalVariable;
    fo<caret>o = new LocalVariable();
  }
}
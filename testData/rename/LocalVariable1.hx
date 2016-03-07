class LocalVariable1 {
  function bar(){
    var foo:LocalVariable1;
    f<caret>oo = new LocalVariable1();
  }
}
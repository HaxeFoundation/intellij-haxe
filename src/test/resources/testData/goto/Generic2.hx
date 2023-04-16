class Generic2<T> {
  public function generate():T{
    return null;
  }

  public function foo(test:Generic2<Generic2<String>>){
    test.generate().generate().char<caret>At(0);
  }
}
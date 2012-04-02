class Generic1<T> {
  public function generate():T{
    return null;
  }

  public function foo(){
    var test = new Generic1<String>();
    test.generate().char<caret>At(0);
  }
}
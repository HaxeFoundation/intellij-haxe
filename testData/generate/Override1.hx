class Override1 extends Foo<Bar> {
<caret>
}

class Foo<T> {
  public function getFoo():T {
    return null;
  }
}

class Bar {

}
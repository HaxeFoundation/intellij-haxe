class Test8<T:Foo>
{
        function a(s:T)
        {
                s.b<caret>;
        }
}

class Foo {
  public function bar(){}
  public function baz(){}
}
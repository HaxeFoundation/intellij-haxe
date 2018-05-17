class Test<T:Foo>
{
        function a(s:T)
        {
                s.fo<caret>o;
        }
}

class Foo {
  function foo(){}
}
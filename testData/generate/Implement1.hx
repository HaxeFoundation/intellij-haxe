class Implement1 implements IFoo<Bar> {
<caret>
}

interface IFoo<T> {
  function getFoo1():T;
  function getFoo2():T;
}

class Bar {}
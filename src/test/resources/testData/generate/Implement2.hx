class Implement2 implements IFoo<Bar> {
<caret>
}

interface IFoo<T> {
  function getFoo():T;
  var varInInterface:T;
}

class Bar {}
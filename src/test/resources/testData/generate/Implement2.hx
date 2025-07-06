class Implement2 implements IFoo<Bar> {
<caret>
}

interface IFoo<T> {
  function getFoo():T;
  var varInInterface:T;
  var propertyInInterface(get, never):T;
}

class Bar {}
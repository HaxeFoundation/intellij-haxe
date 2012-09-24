class Implement2 implements IFoo<String> {
<caret>
}

interface IFoo<T> {
  function getFoo():T;
  var varInInterface:T;
}
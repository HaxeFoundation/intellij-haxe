package;
class Test{
  var <error descr="Type required for member variable somevar">somevar</error>;
  function new() {
    somevar = 3.1;
  }
}
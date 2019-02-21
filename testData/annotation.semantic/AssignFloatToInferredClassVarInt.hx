package;

class Test{
  var somevar = 13;

  function new() {
    somevar = <error descr="Incompatible type: Float should be Int">3.14</error>;
  }
}
class NonConstantArgument {
  public function new() {}
  function test(arg:NonConstantArgument <error descr="Parameter default type should be constant but was 'NonConstantArgument'">= new NonConstantArgument()</error>) {
  }
}
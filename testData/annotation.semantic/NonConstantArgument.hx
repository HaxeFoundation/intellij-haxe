class NonConstantArgument {
  function test(<error descr="Parameter default type should be constant but was NonConstantArgument">arg:NonConstantArgument = new NonConstantArgument()</error>) {
  }
}
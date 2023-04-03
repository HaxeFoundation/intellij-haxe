package ;

interface IFoo {
  var variable:Dynamic;
}

class Foo implements IFoo {
  var variable:Dynamic;
}

class Bar extends Foo {
  <error descr="Redefinition of variable 'variable' in subclass is not allowed. Previously declared at 'Foo'.">var variable:Dynamic;</error>
}

class BarString extends Foo {
  <error descr="Redefinition of variable 'variable' in subclass is not allowed. Previously declared at 'Foo'.">var variable:String;</error>
}
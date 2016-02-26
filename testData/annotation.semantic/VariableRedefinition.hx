package ;

interface IFoo {
  var variable:Dynamic;
}

class Foo implements IFoo {
  var variable:Dynamic;
}

class Bar extends Foo {
  var <error descr="Redefinition of variable 'variable' in subclass is not allowed. Previously declared at 'Foo'.">variable:Dynamic</error>;
}

class BarString extends Foo {
  var <error descr="Redefinition of variable 'variable' in subclass is not allowed. Previously declared at 'Foo'.">variable:String</error>;
}
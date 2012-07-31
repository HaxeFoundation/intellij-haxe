typedef Foo = Bar

class Bar {
  var bar;
}

class TypeDef4 extends Foo {
  function main(){
    ba<caret>r;
  }
}
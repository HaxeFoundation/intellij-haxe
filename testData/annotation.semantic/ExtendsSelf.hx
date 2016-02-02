typedef Foo2 = {
  ><error descr="Cannot extend self">Foo2</error>
};

class Bar extends <error descr="Cannot extend self">Bar</error> {

}

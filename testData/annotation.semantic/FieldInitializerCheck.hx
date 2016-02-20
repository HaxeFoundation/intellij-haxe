interface IBar {}
class Bar implements IBar {}
class Bar2 extends Bar {}
class Bar3 extends <error descr="Cannot extend self">Bar3</error> {}

class FieldInitializerCheck {
  var ok1:IBar = new Bar2();
  var ok2:IBar = new Bar();
  var ok3:Bar = new Bar2();
  <error descr="Incompatible type Bar2 can't be assigned from Bar">var fail1:Bar2 = new Bar();</error>
  <error descr="Incompatible type Bar2 can't be assigned from Bar3">var fail2:Bar2 = new Bar3();</error>
}
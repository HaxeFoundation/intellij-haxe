interface IBar {}
class Bar implements IBar {public function new(){}}
class Bar2 extends Bar {public function new(){super();}}
class Bar3 extends <error descr="Cannot extend self">Bar3</error> {public function new(){super();}}
class Bar4 extends Bar5 {public function new(){super();}}
class Bar5 extends Bar4 {public function new(){super();}}

class FieldInitializerCheck {
  var ok1:IBar = new Bar2();
  var ok2:IBar = new Bar();
  var ok3:Bar = new Bar2();
  <error descr="Incompatible type: Bar should be Bar2">var fail1:Bar2 = new Bar();</error>
  <error descr="Incompatible type: Bar3 should be Bar2">var fail2:Bar2 = new Bar3();</error>
  <error descr="Incompatible type: Bar5 should be IBar">var fail3:IBar = new Bar5();</error>
}
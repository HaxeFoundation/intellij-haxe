interface IBar {}
class Bar implements IBar {
  public function new() {}
}
class Bar2 extends Bar {}
class Bar3 extends <error descr="Cannot extend self">Bar3</error> {
  public function new() {}
}
class Bar4 extends Bar5 {}
class Bar5 extends Bar4 {
  public function new() {}
}

class FieldInitializerCheck {
  var ok1:IBar = new Bar2();
  var ok2:IBar = new Bar();
  var ok3:Bar = new Bar2();
  <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Bar2</b></td></tr><tr><td>Found:</td><td><font color='red'><b>Bar</b></font></td></tr></table></body></html>">var fail1:Bar2 = new Bar();</error>
  <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Bar2</b></td></tr><tr><td>Found:</td><td><font color='red'><b>Bar3</b></font></td></tr></table></body></html>">var fail2:Bar2 = new Bar3();</error>
  <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>IBar</b></td></tr><tr><td>Found:</td><td><font color='red'><b>Bar5</b></font></td></tr></table></body></html>">var fail3:IBar = new Bar5();</error>
}
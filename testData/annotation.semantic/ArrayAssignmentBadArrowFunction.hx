package;
class Test {
  <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Array<Int->Int></b></td></tr><tr><td>Found:</td><td><font color='red'><b>Array<Int->Float></b></font></td></tr></table></body></html>">var should_warn3: Array<Int->Int> = [ a -> 1.0 ];</error>
  public function new() {
    var <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Array<Int->Int></b></td></tr><tr><td>Found:</td><td><font color='red'><b>Array<Int->Float></b></font></td></tr></table></body></html>">should_warn3: Array<Int->Int> = [ a -> 1.0 ]</error>;
  }
}
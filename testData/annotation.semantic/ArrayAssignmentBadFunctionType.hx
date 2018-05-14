package;
class Test {
  <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Array<Int->Int></b></td></tr><tr><td>Found:</td><td><font color='red'><b>Array<Int->String></b></font></td></tr></table></body></html>">var should_warn1: Array<Int->Int> = [ (x:Int)->{'$x'} ];</error>
  public function new() {
    var <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Array<Int->Int></b></td></tr><tr><td>Found:</td><td><font color='red'><b>Array<Int->String></b></font></td></tr></table></body></html>">should_warn1: Array<Int->Int> = [ (x:Int)->{'$x'} ]</error>;
  }
}
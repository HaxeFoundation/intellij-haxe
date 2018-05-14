class ChangeArgumentType {
  static public function test(<error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Int</b></td></tr><tr><td>Found:</td><td><font color='red'><b>Bool</b></font></td></tr></table></body></html>">a:Int = <caret>false</error>) {

  }
}
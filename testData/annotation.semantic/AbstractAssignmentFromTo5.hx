package ;

class AbstractAssignmentFromTo1 {
  var my:MyArray<Int> = [10];
  <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Array<Int></b></td></tr><tr><td>Found:</td><td><font color='red'><b>MyArray<Int></b></font></td></tr></table></body></html>">var arr:Array<Int> = my;</error>
}

abstract MyArray<T>(Array<T>) from Array<T> {
}
package ;

class AbstractAssignmentFromTo1 {
  public static function test():Void {
    var <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>MyArray<Int></b></td></tr><tr><td>Found:</td><td><font color='red'><b>Array<Int></b></font></td></tr></table></body></html>">val:MyArray<Int> = [10]</error>;
  }
}

abstract MyArray<T>(Array<T>) {
}


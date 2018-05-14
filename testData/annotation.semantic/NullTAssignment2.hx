class TypedefNullTAssignment {
  <error descr="<html><body>Incompatible types.<table><tr><td>Expected:</td><td><b>Null<Int></b></td></tr><tr><td>Found:</td><td><font color='red'><b>String</b></font></td></tr></table></body></html>">var a:Null<Int> = "String";</error>
}
typedef Null<T> = T;

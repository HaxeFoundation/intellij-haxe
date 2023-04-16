package ;

class AbstractAssignmentFromTo1 {
  var my:MyArray<Int> = [10];
  <error descr="Incompatible type: MyArray<Int> should be Array<Int>">var arr:Array<Int> = my;</error>
}

abstract MyArray<T>(Array<T>) from Array<T> {
}
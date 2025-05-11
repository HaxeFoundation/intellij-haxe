package ;

class AbstractAssignmentFromTo1 {
  var my:MyArray<Int> = [10];
  var arr:Array<Int> = <error descr="Incompatible type: MyArray<Int> should be Array<Int>">my</error>;
}

abstract MyArray<T>(Array<T>) from Array<T> {
}
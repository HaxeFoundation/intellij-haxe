package ;

class AbstractAssignmentFromTo1 {
  var my:MyArray<Int> = [10];
  <error descr="Incompatible type Array<Int> can't be assigned from MyArray<Int>">var arr:Array<Int> = my;</error>
}

abstract MyArray<T>(Array<T>) from Array<T> {
}
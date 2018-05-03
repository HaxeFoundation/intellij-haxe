package ;

class AbstractAssignmentFromTo1 {
  public static function test():Void {
    var val:MyArray2<Int> = [10];
  }
}

abstract MyArray2<T>(MyArray<T>) from MyArray<T> to MyArray<T> {
}

abstract MyArray<T>(Array<T>) from Array<T> to Array<T> {
}
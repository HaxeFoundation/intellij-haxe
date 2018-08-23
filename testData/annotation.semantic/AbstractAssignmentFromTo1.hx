package ;

class AbstractAssignmentFromTo1 {
  public static function test():Void {
    var val:MyArray<Int> = [10];
  }
}

abstract MyArray<T>(Array<T>) from Array<T> {
}
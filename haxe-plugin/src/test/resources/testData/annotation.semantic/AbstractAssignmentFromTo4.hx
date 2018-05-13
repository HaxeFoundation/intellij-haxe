package ;

class AbstractAssignmentFromTo1 {
  public static function test():Void {
    var my:MyArray<Int> = [10];
    var arr:Array<Int> = my;
  }
}

abstract MyArray<T>(Array<T>) from Array<T> to Array<T> {
}
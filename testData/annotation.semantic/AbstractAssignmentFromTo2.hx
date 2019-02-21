package ;

class AbstractAssignmentFromTo1 {
  public static function test():Void {
    var <error descr="Incompatible type: Array<Int> should be MyArray<Int>">val:MyArray<Int> = [10]</error>;
  }
}

abstract MyArray<T>(Array<T>) {
}


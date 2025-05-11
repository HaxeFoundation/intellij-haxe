package ;

class AbstractAssignmentFromTo1 {
  public static function test():Void {
    var val:MyArray<Int> = <error descr="Incompatible type: Array<Int> should be MyArray<Int>">[10]</error>;
  }
}

abstract MyArray<T>(Array<T>) {
}


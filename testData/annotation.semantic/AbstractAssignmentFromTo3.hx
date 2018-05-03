package ;

class AbstractAssignmentFromTo1 {
  public static function test():Void {
    var <error descr="Incompatible type MyArray<Int> can't be assigned from Array<Int>">val:MyArray<Int> = [10]</error>;
  }
}

abstract MyArray<T>(Array<T>) to Array<T>{
}


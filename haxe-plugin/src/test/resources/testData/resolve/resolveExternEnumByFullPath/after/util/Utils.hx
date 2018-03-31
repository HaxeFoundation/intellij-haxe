package util;
class Utils {
  public static function delete_negative(array:Array<Int>):Array<Int> {
    return ArrayUtils.delete_if(array, function(value:Int):Bool {
      return value < 0;
    });
  }
}

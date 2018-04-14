package;
class Test {
  var arr: Array<Int->Int> = [ (x:Int)->{x} ];
  var arr1: Array<Int->Int> = [ x->x ];
  var arr1b: Array<Int->Int> = [ function (x) x ];

  public function new() {
      var arr: Array<Int->Int> = [ (x:Int)->{x} ];
      var arr1: Array<Int->Int> = [ x->x ]; // x->x currently is evaluated as Unknown->Unknown
      var arr1b: Array<Int->Int> = [ function (x) x ];
  }
}
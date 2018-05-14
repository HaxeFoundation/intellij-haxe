class Implementation implements <error descr="Not i">InterA {
}

interface InterA extends InterB {
  var x(get, set):Int;
  function a():Int;
}

interface InterB {
  var y:String;
  function b(val:String):Void;
}

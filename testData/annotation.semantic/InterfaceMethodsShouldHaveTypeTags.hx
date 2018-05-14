interface InterfaceMethodsShouldHaveTypeTags {
  function correct(a:Int, b:String):Int;
  function <error descr="Return type required for method declared in interface">incorrectReturn</error>(a:Int, b:String);
  function incorrectArguments(<error descr="Parameter type required for method declared in interface">a</error>, b:Bool, <error descr="Parameter type required for method declared in interface">c</error>):Int;
}
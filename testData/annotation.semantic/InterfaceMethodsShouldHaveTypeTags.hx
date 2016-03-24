interface InterfaceMethodsShouldHaveTypeTags {
  function correct(a:Int, b:String):Int;
  function <error descr="Type required for extern classes and interfaces">incorrectReturn</error>(a:Int, b:String);
  function incorrectArguments(<error descr="Type required for extern classes and interfaces">a</error>, b:Bool, <error descr="Type required for extern classes and interfaces">c</error>):Int;
}
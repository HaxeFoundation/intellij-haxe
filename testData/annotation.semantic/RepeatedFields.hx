class Test {
  public function <error descr="Duplicate class member declaration 'a'">a</error>(str:Int) {}
  public function <error descr="Duplicate class member declaration 'a'">a</error>(str:Int) {}

  function <error descr="Duplicate class member declaration 'b'">b</error>() {}
  function <error descr="Duplicate class member declaration 'b'">b</error>() {}

  var <error descr="Duplicate class member declaration 'c'">c</error>:Int;
  var <error descr="Duplicate class member declaration 'c'">c</error>:String;

  var <error descr="Duplicate class member declaration 'd'">d</error>(null, never):Int;
  var <error descr="Duplicate class member declaration 'd'">d</error>(null, never):Int;

  var <error descr="Duplicate class member declaration 'e'">e</error>:Int;
  var <error descr="Duplicate class member declaration 'e'">e</error>:String;
  var <error descr="Duplicate class member declaration 'e'">e</error>:Test;
}


class Test {
  public function <error descr="Duplicate class field declaration : a">a</error>(str:Int) {}
  public function <error descr="Duplicate class field declaration : a">a</error>(str:Int) {}

  function <error descr="Duplicate class field declaration : b">b</error>() {}
  function <error descr="Duplicate class field declaration : b">b</error>() {}

  var <error descr="Duplicate class field declaration : c">c</error>:Int;
  var <error descr="Duplicate class field declaration : c">c</error>:String;

  var <error descr="Duplicate class field declaration : d">d</error>(null, never):Int;
  var <error descr="Duplicate class field declaration : d">d</error>(null, never):Int;

  var <error descr="Duplicate class field declaration : e">e</error>:Int;
  var <error descr="Duplicate class field declaration : e">e</error>:String;
  var <error descr="Duplicate class field declaration : e">e</error>:Test;
}


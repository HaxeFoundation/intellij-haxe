// To verify with haxe compiler: In this directory, run "haxe --interp Imports.hx --main Imports"

package;

class <info descr="">Imports</info> {
  var <info descr="">helper</info> : <info descr="">Helper</info> = <info descr="">new</info> <info descr="">Helper</info>();  // Brought in by imports.hx

  public function <info descr="">new</info>() {}

  public static function <info descr="">main</info>() {
    var <info descr="">t</info> = <info descr="">new</info> <info descr="">Imports</info>();
    for (<info descr="">i</info> <info descr="">in</info> 1 ... 3)
      <info descr=""><info descr=""><info descr="">t</info>.helper</info>.push</info>(<info descr="">i</info>);
    trace("length = " + <info descr=""><info descr=""><info descr="">t</info>.helper</info>.length</info>());
    while (0 < <info descr=""><info descr=""><info descr="">t</info>.helper</info>.length</info>())
      trace(<info descr=""><info descr=""><info descr="">t</info>.helper</info>.pop</info>());

    <info descr=""><info descr=""><info descr="">t</info>.helper</info>.callme</info>(<info descr="">t</info>);
  }

  public function <info descr="">answer</info>() {  // Demonstrates two levels of import.hx work from Helper.hx.
    trace("answer called.");
    <info descr="">"Hello? Anybody there?".show</info>();
  }
}
// To verify with haxe compiler: In this directory, run "haxe --interp Imports.hx --main Imports"

package;

class <info descr="">Imports</info> {
  var <info descr="">helper</info> : <info descr="">Helper</info> = <info descr="">new</info> <info descr="">Helper</info>();  // Brought in by imports.hx

  public function <info descr="">new</info>() {}

  public static function <info descr="">main</info>() {
    var <info descr="">t</info> = <info descr="">new</info> <info descr="">Imports</info>();
    for (<info descr="">i</info> <info descr="">in</info> 1 ... 3)
      <info descr="">t</info>.<info descr="">helper</info>.<info descr="">push</info>(<info descr="">i</info>);
    trace("length = " + <info descr="">t</info>.<info descr="">helper</info>.<info descr="">length</info>());
    while (0 < <info descr="">t</info>.<info descr="">helper</info>.<info descr="">length</info>())
      trace(<info descr="">t</info>.<info descr="">helper</info>.<info descr="">pop</info>());

    <info descr="">t</info>.<info descr="">helper</info>.<info descr="">callme</info>(<info descr="">t</info>);
  }

  public function <info descr="">answer</info>() {  // Demonstrates two levels of import.hx work from Helper.hx.
    trace("answer called.");
    "Hello? Anybody there?".<info descr="">show</info>();
  }
}
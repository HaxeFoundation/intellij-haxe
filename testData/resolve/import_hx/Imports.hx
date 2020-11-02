// To verify with haxe compiler: In this directory, run "haxe --interp Imports.hx --main Imports"

package;

class <info descr="null">Imports</info> {
  var <info descr="null">helper</info> : <info descr="null">Helper</info> = <info descr="null">new</info> <info descr="null">Helper</info>();  // Brought in by imports.hx

  public function <info descr="null">new</info>() {}

  public static function <info descr="null">main</info>() {
    var <info descr="null">t</info> = <info descr="null">new</info> <info descr="null">Imports</info>();
    for (<info descr="null">i</info> <info descr="null">in</info> 1 ... 3)
      <info descr="null">t</info>.<info descr="null">helper</info>.<info descr="null">push</info>(<info descr="null">i</info>);
    trace("length = " + <info descr="null">t</info>.<info descr="null">helper</info>.<info descr="null">length</info>());
    while (0 < <info descr="null">t</info>.<info descr="null">helper</info>.<info descr="null">length</info>())
      trace(<info descr="null">t</info>.<info descr="null">helper</info>.<info descr="null">pop</info>());

    <info descr="null">t</info>.<info descr="null">helper</info>.<info descr="null">callme</info>(<info descr="null">t</info>);
  }

  public function <info descr="null">answer</info>() {  // Demonstrates two levels of import.hx work from Helper.hx.
    trace("answer called");
  }
}
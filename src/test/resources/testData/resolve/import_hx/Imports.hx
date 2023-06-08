// To verify with haxe compiler: In this directory, run "haxe --interp Imports.hx --main Imports"

package;

class <text_attr descr="null">Imports</text_attr> {
  var <text_attr descr="null">helper</text_attr> : <text_attr descr="null">Helper</text_attr> = <text_attr descr="null">new</text_attr> <text_attr descr="null">Helper</text_attr>();  // Brought in by imports.hx

  public function <text_attr descr="null">new</text_attr>() {}

  public static function <text_attr descr="null">main</text_attr>() {
    var <text_attr descr="null">t</text_attr> = <text_attr descr="null">new</text_attr> <text_attr descr="null">Imports</text_attr>();
    for (<text_attr descr="null">i</text_attr> <text_attr descr="null">in</text_attr> 1 ... 3)
      <text_attr descr="null">t</text_attr>.<text_attr descr="null">helper</text_attr>.<text_attr descr="null">push</text_attr>(<text_attr descr="null">i</text_attr>);
    trace("length = " + <text_attr descr="null">t</text_attr>.<text_attr descr="null">helper</text_attr>.<text_attr descr="null">length</text_attr>());
    while (0 < <text_attr descr="null">t</text_attr>.<text_attr descr="null">helper</text_attr>.<text_attr descr="null">length</text_attr>())
      trace(<text_attr descr="null">t</text_attr>.<text_attr descr="null">helper</text_attr>.<text_attr descr="null">pop</text_attr>());

    <text_attr descr="null">t</text_attr>.<text_attr descr="null">helper</text_attr>.<text_attr descr="null">callme</text_attr>(<text_attr descr="null">t</text_attr>);
  }

  public function <text_attr descr="null">answer</text_attr>() {  // Demonstrates two levels of import.hx work from Helper.hx.
    trace("answer called.");
    "Hello? Anybody there?".<text_attr descr="null">show</text_attr>();
  }
}
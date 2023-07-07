package;

class <info descr="null">Test</info> {
  function <info descr="null">demo4</info>() {
    //(new haxe.Timer(1000)).run();  // <- run was unresolved
    (<info descr="null">new</info> <info descr="null">Map</info><<info descr="null">String</info>,<info descr="null">String</info>>()).<info descr="null">keys</info>();  // No Timer in test Std, so use Map.
  }
}
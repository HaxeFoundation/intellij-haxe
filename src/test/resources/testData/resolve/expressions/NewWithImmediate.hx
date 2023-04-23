package;

class <info descr="">Test</info> {
  function <info descr="">demo4</info>() {
    //(new haxe.Timer(1000)).run();  // <- run was unresolved
    (<info descr="">new</info> <info descr="">Map</info><<info descr="">String</info>,<info descr="">String</info>>()).<info descr="">keys</info>();  // No Timer in test Std, so use Map.
  }
}
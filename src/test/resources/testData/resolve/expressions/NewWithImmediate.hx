package;

class <text_attr descr="null">Test</text_attr> {
  function <text_attr descr="null">demo4</text_attr>() {
    //(new haxe.Timer(1000)).run();  // <- run was unresolved
    (<text_attr descr="null">new</text_attr> <text_attr descr="null">Map</text_attr><<text_attr descr="null">String</text_attr>,<text_attr descr="null">String</text_attr>>()).<text_attr descr="null">keys</text_attr>();  // No Timer in test Std, so use Map.
  }
}
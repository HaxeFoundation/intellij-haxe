package ;

import StdTypes;
import <info descr="null">Map</info>;
import <info descr="null">String</info>;

class <info descr="null">Test</info> {
  public function <info descr="null">test</info>() {
    var <info descr="null">a</info> = [ 1 => "one", "TWO" => 2.0, 3.0 => function(){return "a string"} ];
    trace(<info descr="null">a</info>[3.0].<warning descr="Unresolved symbol">length</warning>); // Unresolved because a is Map<Dynamic,Unknown>
  }
}
package ;

import StdTypes;
import <text_attr descr="null">Map</text_attr>;
import <text_attr descr="null">String</text_attr>;

class <text_attr descr="null">Test</text_attr> {
  public function <text_attr descr="null">test</text_attr>() {
    var <text_attr descr="null">a</text_attr> = [ 1 => "one", "TWO" => 2.0, 3.0 => function(){return "a string"<error descr="Missing semicolon.">}</error> ];
    trace(<text_attr descr="null">a</text_attr>[3.0].<warning descr="Unresolved symbol">length</warning>); // Unresolved because a is Map<Dynamic,Unknown>
  }
}
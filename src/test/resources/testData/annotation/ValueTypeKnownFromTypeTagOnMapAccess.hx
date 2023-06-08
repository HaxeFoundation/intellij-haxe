// This is a negative test where no errors should be marked.
package ;

import StdTypes;
import <text_attr descr="null">Map</text_attr>;
import <text_attr descr="null">String</text_attr>;

class <text_attr descr="null">Test</text_attr> {
  function <text_attr descr="null">doTest</text_attr>() {
    var <text_attr descr="null">a</text_attr> : <text_attr descr="null">Map</text_attr><<text_attr descr="null">String</text_attr>, <text_attr descr="null">String</text_attr>> = [ "ONE" => "one", "TWO" => "two" ];
    trace(<text_attr descr="null">a</text_attr>["ONE"].<text_attr descr="null">length</text_attr>);
  }
}
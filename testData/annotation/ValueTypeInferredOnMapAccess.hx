// This is a negative test, wherein no errors should be highlighted.
package;

import StdTypes;
import <info descr="null">Map</info>;
import <info descr="null">String</info>;

class <info descr="null">Test</info> {
  function <info descr="null">Test</info>() {
    var <info descr="null">a</info> = ["ONE" => "one", "TWO" => "two"];
    trace(<info descr="null">a</info>["ONE"].<info descr="null">length</info>);
  }
}
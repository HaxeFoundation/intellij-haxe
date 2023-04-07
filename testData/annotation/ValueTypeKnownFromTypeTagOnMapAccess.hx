// This is a negative test where no errors should be marked.
package ;

import StdTypes;
import <info descr="">Map</info>;
import <info descr="">String</info>;

class <info descr="">Test</info> {
  function <info descr="">doTest</info>() {
    var <info descr="">a</info> : <info descr="">Map</info><<info descr="">String</info>, <info descr="">String</info>> = [ "ONE" => "one", "TWO" => "two" ];
    trace(<info descr=""><info descr="">a</info>["ONE"].length</info>);
  }
}
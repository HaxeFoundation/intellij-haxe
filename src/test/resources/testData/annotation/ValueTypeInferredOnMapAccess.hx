// This is a negative test, wherein no errors should be highlighted.
package;

import StdTypes;
import <info descr="">Map</info>;
import <info descr="">String</info>;
import <info descr=""><info descr="">haxe.ds</info>.StringMap</info>;

class <info descr="">Test</info> {
  function <info descr="">Test</info>() {
    var <info descr="">a</info> = ["ONE" => "one", "TWO" => "two"];
    trace(<info descr=""><info descr="">a</info>["ONE"].length</info>);

    var <info descr="">b</info> : <info descr="">StringMap</info><<info descr="">String</info>> = <info descr="">a</info>;
  }
}
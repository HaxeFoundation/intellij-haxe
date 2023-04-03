// This is a negative test, wherein no errors should be highlighted.
package;

import StdTypes;
import <info descr="null">Map</info>;
import <info descr="null">String</info>;
import <info descr="null"><info descr="null">haxe.ds</info>.StringMap</info>;

class <info descr="null">Test</info> {
  function <info descr="null">Test</info>() {
    var <info descr="null">a</info> = ["ONE" => "one", "TWO" => "two"];
    trace(<info descr="null">a</info>["ONE"].<info descr="null">length</info>);

    var <info descr="null">b</info> : <info descr="null">StringMap</info><<info descr="null">String</info>> = <info descr="null">a</info>;
  }
}
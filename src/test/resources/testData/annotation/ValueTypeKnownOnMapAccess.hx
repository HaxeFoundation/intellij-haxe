// This is a negative test where no errors should be marked.
package ;

import StdTypes;
import <info descr="null">Map</info>;
import <info descr="null">String</info>;

class <info descr="null">Test</info> {
  function <info descr="null">doTest</info>() {
    var <info descr="null">a</info> = <info descr="null">new</info> <info descr="null">Map</info><<info descr="null">String</info>, <info descr="null">String</info>>();
    <info descr="null">a</info>["ONE"] = "one";
    <info descr="null">a</info>.<info descr="null">set</info>("TWO", "two");

    trace(<info descr="null">a</info>["ONE"].<info descr="null">length</info>);
  }
}
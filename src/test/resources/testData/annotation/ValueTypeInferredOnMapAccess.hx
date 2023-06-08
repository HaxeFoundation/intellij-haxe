// This is a negative test, wherein no errors should be highlighted.
package;

import StdTypes;
import <text_attr descr="null">Map</text_attr>;
import <text_attr descr="null">String</text_attr>;
import <text_attr descr="null"><text_attr descr="null">haxe.ds</text_attr>.StringMap</text_attr>;

class <text_attr descr="null">Test</text_attr> {
  function <text_attr descr="null">Test</text_attr>() {
    var <text_attr descr="null">a</text_attr> = ["ONE" => "one", "TWO" => "two"];
    trace(<text_attr descr="null">a</text_attr>["ONE"].<text_attr descr="null">length</text_attr>);

    var <text_attr descr="null">b</text_attr> : <text_attr descr="null">StringMap</text_attr><<text_attr descr="null">String</text_attr>> = <text_attr descr="null">a</text_attr>;
  }
}
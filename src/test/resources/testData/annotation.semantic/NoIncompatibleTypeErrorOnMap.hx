// File should not have any errors.
package;

class Test {
  private var myMap : Map<String, String>;

  public function new() {
    myMap = [ "something" => "somevalue",
              "else" => "elsevalue"
            ];
  }
  public function test() {
    var val : String = myMap.get("something");  // Original issue was: Null<Unknown> should be String.
  }
}

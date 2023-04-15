// File should not have any errors.
package;

class Test {
  private var myMap : Map<Int, Map<String,String>>;


  public function new() {
    myMap = [1 => [ "something" => "somevalue",
              "else" => "elsevalue"
                ]
            ];
  }
  public function test() {
    var val : String = myMap.get(1).get("something");
  }
}

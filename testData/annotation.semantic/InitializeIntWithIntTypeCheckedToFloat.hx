// This is a negative test; an error should NOT occur.
package;
class Test {
  public function new() {
    // The following is OK because an Int unifies with a Float, but is still an Int.
    var i:Int = (10:Float);
  }
}
import haxe.Exception;
import test.AbstractEnum;
class Main {
  public function new() {
    try {
      var x:AbstractEnum;
      switch (x) {
        // verify resolves to : enum value
        case Exception : trace("Exception");
        default :  trace("default");
      }
      // verify resolves to : enum value
      if (AbstractEnum.Exception == null) {
        // verify resolves to : Type
        throw new Exception ("");
      }
      // verify resolves to : Type
    } catch (e:Exception) {}
  }
}



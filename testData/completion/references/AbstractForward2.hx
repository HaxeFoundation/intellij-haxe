import com.util.UnderlyingType;
@:forward
abstract AbstractForward2(UnderlyingType) from UnderlyingType to UnderlyingType {

  inline public function new(v:UnderlyingType) {
    this = v;
  }

}

class Test {

  public function new() {
    var a:AbstractForward1 = new UnderlyingType();
    a.<caret>
  }

}
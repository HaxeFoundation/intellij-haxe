import com.util.UnderluingType;
@:forward(i<caret>)
abstract AbstractForward(UnderlyingType) from UnderlyingType to UnderlyingType {

  inline public function new(v:UnderlyingType) {
    this = v;
  }

}
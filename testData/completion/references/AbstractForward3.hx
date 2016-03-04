import com.util.UnderlyingType;
@:forward
abstract AbstractForward2(UnderlyingType) from UnderlyingType to UnderlyingType {

  inline public function new(v:UnderlyingType) {
    this = v;
    this.<caret>
  }

}
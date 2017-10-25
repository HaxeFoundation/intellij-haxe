package;
import com.bar.TestSearchOverrides;

class OverrideTop extends Top {
  public function new() {super();}
  public override function play() {
    super.p<caret>lay();
  }
}

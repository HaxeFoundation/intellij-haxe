// getWidth exists only after the build macro ran
@:build(AccessorMacro.build())
class Settings {
	@:accessor var width:Int = 800;

	public function new() {}
}

class Main {
	@:noCompletion private var __buffer:Surface;
	@:noCompletion private var __bufferWidth:Int;

	@:noCompletion private static var __buffers:Array<Surface> = [];

	// the surface being drawn into when it is ours
	@:noCompletion private var __target:Surface;

	@:noCompletion private static var __groupSurfaces:Array<Surface> = [];
	@:noCompletion private static var __mask:Pattern;

	private var __plane:Surface;

	public var levels:Int;
	public var depth:Int;

	private var __planeRect:Rect;

	public function new() {}
}

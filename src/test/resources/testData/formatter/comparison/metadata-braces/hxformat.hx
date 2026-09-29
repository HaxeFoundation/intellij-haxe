class Main
{
	@:noCompletion private var __matrix:Matrix;

	@SuppressWarnings("checkstyle:Dynamic")
	@:noCompletion private function new(matrix:Matrix)
	{
		super();
		__matrix = matrix;
	}

	@:noCompletion private function apply(transform:Matrix):Void
	{
		if (transform != null)
		{
			__matrix = transform;
		}
	}

	@:noCompletion private function composite(surface:CanvasElement, level:Int, horizontal:Int, vertical:Int, width:Int, height:Int, blendKind:BlendKind,
			displayObject:DisplayObject):Void
	{
		trace(surface);
	}

	@:keep
	private function reset():Void
	{
		__matrix = null;
	}

	public function noop() {}
}

package;

import haxe.io.Bytes;
#if native
import app.render.Image;
#end

class Main {
	#if native
	function blend(alpha:Float):Void {
		if (alpha >= 1)
			paint();
		else
			paintAlpha(alpha);

		for (child in children)
			draw(child);
	}
	#end

	function touch():Void {
		trace(Bytes.alloc(1));
	}
}

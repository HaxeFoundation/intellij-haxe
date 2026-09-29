class Main {
	#if native
	function blend(alpha:Float):Void {
		#if debug
		if (alpha > 0) {
			trace("debug");
		}
		#else
		trace("release");
		#end
	}
	#end

	static function main() {
		#if js
		trace("js");
		#if debug
		trace("debug");
		#else
		trace("release");
		#end
		trace("after");
		#end
		trace("live");
	}
}

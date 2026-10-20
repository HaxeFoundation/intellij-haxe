class Main {
	public function getSurface(): #if native NativeSurface#else Dynamic #end{
		return null;
	}

	static function main() {
		var source:Dynamic = null;
		if ( #if (haxe_ver >= 4.2) Std.isOfType #else Std.is#end(source, String)) {
			trace("string");
		}
		var mode = #if debug"dev" #else "prod"#end ;
		var first = #if debug source #else mode #end[0];
		trace(#if debug 1 #else 2 #end , 3);
	}
}

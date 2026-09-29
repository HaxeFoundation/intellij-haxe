class Main {
	static function main() {
			#if js
		trace("js");
	#elseif cpp
		trace("cpp");
			#else
	trace("other");
		#end
		var mode = #if debug "dev" #else "prod" #end;
		trace(mode);
	}
}

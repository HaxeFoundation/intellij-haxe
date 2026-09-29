package ijhaxe;

#if (cpp && HXCPP_TELEMETRY && !HXCPP_TRACY)
/**
	Haxe access to hxcpp's telemetry C API (hx/Telemetry.h, which hxcpp.h
	includes under HXCPP_TELEMETRY). `dumpInto` only copies a frame's raw
	vectors into Haxe arrays; TelemetryRun does all the serialization, which
	keeps the injected C++ trivial.
**/
class CppTelemetry {
	public static function start():Int {
		return untyped __global__.__hxcpp_hxt_start_telemetry(true, false);
	}

	public static function stash():Void {
		untyped __global__.__hxcpp_hxt_stash_telemetry();
	}

	public static function usedBytes():Int {
		return untyped __global__.__hxcpp_gc_used_bytes();
	}

	public static function reservedBytes():Int {
		return untyped __global__.__hxcpp_gc_reserved_bytes();
	}

	@:functionCode('
		TelemetryFrame* frame = __hxcpp_hxt_dump_telemetry(thread_num);
		if (frame == 0) return false;
		gcTimes->push((int)frame->gctime);
		gcTimes->push((int)frame->gcoverhead);
		if (frame->names != 0) {
			int size = (int)frame->names->size();
			for (int i = 0; i < size; i++) names->push(String(frame->names->at(i)));
		}
		if (frame->samples != 0) {
			int size = (int)frame->samples->size();
			for (int i = 0; i < size; i++) samples->push((int)frame->samples->at(i));
		}
		return true;
	')
	public static function dumpInto(thread_num:Int, gcTimes:Array<Int>, names:Array<String>, samples:Array<Int>):Bool {
		return false;
	}
}
#end

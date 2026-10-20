package ijhaxe.debug.inspect;
import ijhaxe.debug.Trace;

import ijhaxe.debug.target.DebugApi;
import ijhaxe.debug.target.Register;
import ijhaxe.debug.values.ValueReader;
import ijhaxe.debug.values.VariableInfo;
import haxe.Int64;

/**
	Reads the architecture-neutral CPU registers (SP, BP, IP, FLAGS) of a
	SUSPENDED thread and formats them as rows of the Registers scope.

	Only these four register indexes are portable across the supported
	platforms (see Register); on Windows the others silently read RAX. Register
	reads are meaningful only while the thread is stopped, so callers must
	check that. A read of a running thread would fault or return garbage; the
	try/catch here swallows the fault.
**/
class CpuRegisters {
	final api:DebugApi;
	final pid:Int;

	public function new(api:DebugApi, pid:Int) {
		this.api = api;
		this.pid = pid;
	}

	/**
		SP, BP, IP and FLAGS of `threadId` as rows; the rows end at the first failed read.
	**/
	public function rows(threadId:Int):Array<VariableInfo> {
		var rows:Array<VariableInfo> = [];
		try {
			var read = (name, register) -> {
				var value = api.readRegister(pid, threadId, register);
				rows.push({name: name, value: ValueReader.hex(value), type: "CPU", reference: 0});
				value;
			};
			read("SP", Esp);
			read("BP", Ebp);
			read("IP", Eip);
			var flags = Int64.toInt(read("FLAGS", EFlags));
			rows[rows.length - 1].value += flagBits(flags);
		} catch (e:Dynamic) {
			Trace.log("cpu register read failed: " + Std.string(e));
		}
		return rows;
	}

	static function flagBits(flags:Int):String {
		var names = [];
		if (flags & 0x001 != 0) names.push("CF");
		if (flags & 0x004 != 0) names.push("PF");
		if (flags & 0x040 != 0) names.push("ZF");
		if (flags & 0x080 != 0) names.push("SF");
		if (flags & 0x100 != 0) names.push("TF");
		if (flags & 0x400 != 0) names.push("DF");
		if (flags & 0x800 != 0) names.push("OF");
		return names.length == 0 ? "" : " [" + names.join(" ") + "]";
	}
}

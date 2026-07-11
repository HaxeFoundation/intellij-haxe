package debug.module;
import dap.protocol.Source;

import debug.DebugError;

import format.hl.Data;
import format.hl.Data.HLType;
import format.hl.Data.Opcode;

/**
 * Reads a .hl file's embedded debug tables (via the `format` haxelib) and maps
 * between source (file, line) and bytecode (function index, opcode).
 *
 * The function index used here is the position in the code's function array,
 * which matches the per-function order of the handshake (see JitInfo), so a
 * (fidx, op) resolved here can be handed straight to JitInfo.addressOf.
 */
class ModuleDebugInfo {
	final data:Data;
	final isWindows:Bool;
	// findex (global) -> "Class.method" display name
	final namesByFindex:Map<Int, String>;
	// findex (global) -> position in data.functions (the index JitInfo uses)
	final functionIndexByFindex:Map<Int, Int>;
	// findex (global) -> the "$Class" statics prototype whose bindings own that function
	final staticsProtoByFindex:Map<Int, ObjPrototype>;
	// statics-container type name (e.g. "$Config") -> its global index (the slot in
	// the global data block that holds the class's statics singleton pointer)
	final globalIndexByTypeName:Map<String, Int>;
	// type name -> module HLType, for resolving runtime hl_type names
	final typesByName:Map<String, HLType>;

	public function new(hlFilePath:String) {
		var bytes = sys.io.File.getBytes(hlFilePath);
		data = new format.hl.Reader().read(new haxe.io.BytesInput(bytes));
		if (!data.flags.has(HasDebug)) {
			throw new DebugError('The program "$hlFilePath" was compiled without debug info; recompile with -debug');
		}
		isWindows = Sys.systemName() == "Windows";
		namesByFindex = buildNames();
		functionIndexByFindex = buildFunctionIndex();
		staticsProtoByFindex = buildStaticsIndex();
		globalIndexByTypeName = buildGlobalTypeIndex();
		typesByName = buildTypeNameIndex();
	}

	/** Module type by its runtime name (class, struct or enum), or null. */
	public function typeByName(name:String):Null<HLType> {
		return typesByName.get(name);
	}

	/** The types of the module's globals, in index order (for the globals table layout). */
	public function globals():Array<HLType> {
		return data.globals;
	}

	/**
	 * The statics container prototype ("$Class") that owns the function at `fidx`,
	 * i.e. the class whose static fields should be shown while stopped in it, or
	 * null when the function has no such container (rare) or `fidx` is invalid.
	 */
	public function staticsProtoForFunction(fidx:Int):Null<ObjPrototype> {
		if (fidx < 0 || fidx >= data.functions.length) {
			return null;
		}
		return staticsProtoByFindex.get(data.functions[fidx].findex);
	}

	/**
	 * The global index whose slot holds the statics singleton for a "$Class"
	 * container prototype, or -1 if none. (The singleton's own type is the
	 * container, so it appears directly as a global of that type.)
	 */
	public function staticsGlobalIndex(proto:ObjPrototype):Int {
		var index = globalIndexByTypeName.get(proto.name);
		return index != null ? index : -1;
	}

	public function functionCount():Int {
		return data.functions.length;
	}

	/** Opcode count of a function, for aligning against the handshake tables. */
	public function opCount(fidx:Int):Int {
		return data.functions[fidx].ops.length;
	}

	/** The decoded opcodes of a function (for control-flow / stepping analysis). */
	public function opcodes(fidx:Int):Array<Opcode> {
		return data.functions[fidx].ops;
	}

	/** The function's type (an HFun), for reading its argument count/types. */
	public function functionType(fidx:Int):HLType {
		return data.functions[fidx].t;
	}

	/** The function's register types (arguments first, then locals). */
	public function registers(fidx:Int):Array<HLType> {
		return data.functions[fidx].regs;
	}

	/** The debug "assigns" table mapping variable names (string index) to opcode positions. */
	public function assignsOf(fidx:Int):Array<{varName:Int, position:Int}> {
		return data.functions[fidx].assigns;
	}

	public function stringAt(index:Int):String {
		return (index >= 0 && index < data.strings.length) ? data.strings[index] : "?";
	}

	/** Number of arguments (including an implicit `this` for instance methods). */
	public function argCount(fidx:Int):Int {
		return switch (data.functions[fidx].t) {
			case HFun(f): f.args.length;
			default: 0;
		}
	}

	/** The destination register written by the opcode at `op`, or -1 if it writes none. */
	public function dstRegister(fidx:Int, op:Int):Int {
		var ops = data.functions[fidx].ops;
		if (op < 0 || op >= ops.length) {
			return -1;
		}
		return switch (ops[op]) {
			case OMov(d, _), OInt(d, _), OFloat(d, _), OBool(d, _), OBytes(d, _), OString(d, _), ONull(d):
				d;
			case OAdd(d, _, _), OSub(d, _, _), OMul(d, _, _), OSDiv(d, _, _), OUDiv(d, _, _),
				OSMod(d, _, _), OUMod(d, _, _), OShl(d, _, _), OSShr(d, _, _), OUShr(d, _, _),
				OAnd(d, _, _), OOr(d, _, _), OXor(d, _, _):
				d;
			case ONeg(d, _), ONot(d, _), OIncr(d), ODecr(d):
				d;
			case OCall0(d, _), OCall1(d, _, _), OCall2(d, _, _, _), OCall3(d, _, _, _, _),
				OCall4(d, _, _, _, _, _), OCallN(d, _, _), OCallMethod(d, _, _), OCallThis(d, _, _),
				OCallClosure(d, _, _):
				d;
			case OStaticClosure(d, _), OInstanceClosure(d, _, _), OVirtualClosure(d, _, _):
				d;
			case OGetGlobal(d, _), OField(d, _, _), OGetThis(d, _), ODynGet(d, _, _):
				d;
			case OToDyn(d, _), OToSFloat(d, _), OToUFloat(d, _), OToInt(d, _), OSafeCast(d, _),
				OUnsafeCast(d, _), OToVirtual(d, _):
				d;
			case OGetUI8(d, _, _), OGetUI16(d, _, _), OGetMem(d, _, _), OGetArray(d, _, _):
				d;
			case ONew(d), OArraySize(d, _), OType(d, _), OGetType(d, _), OGetTID(d, _), ORef(d, _),
				OUnref(d, _):
				d;
			case OMakeEnum(d, _, _), OEnumAlloc(d, _), OEnumIndex(d, _), OEnumField(d, _, _, _):
				d;
			case ORefData(d, _), ORefOffset(d, _, _):
				d;
			default:
				-1;
		}
	}

	/** Source line of a single opcode, or 0 when unknown. */
	public function lineOf(fidx:Int, op:Int):Int {
		if (fidx < 0 || fidx >= data.functions.length) {
			return 0;
		}
		var debug = data.functions[fidx].debug;
		var index = (op << 1) + 1;
		return (debug != null && index < debug.length) ? debug[index] : 0;
	}

	/**
	 * For a static call opcode (OCall0..4 / OCallN), the callee's function index
	 * (the same index space JitInfo uses). Returns -1 for non-calls and for
	 * dynamic/virtual/closure calls whose target isn't statically known, in which
	 * case step-in falls back to step-over behaviour.
	 */
	public function callTargetFunction(fidx:Int, op:Int):Int {
		if (fidx < 0 || fidx >= data.functions.length) {
			return -1;
		}
		var ops = data.functions[fidx].ops;
		if (op < 0 || op >= ops.length) {
			return -1;
		}
		var findex = switch (ops[op]) {
			case OCall0(_, i), OCall1(_, i, _), OCall2(_, i, _, _), OCall3(_, i, _, _, _),
				OCall4(_, i, _, _, _, _), OCallN(_, i, _):
				i;
			default:
				-1;
		}
		if (findex < 0) {
			return -1;
		}
		var index = functionIndexByFindex.get(findex);
		return index != null ? index : -1;
	}

	/**
	 * Resolves a source breakpoint to bytecode locations, one per function that
	 * has code on that line. When the exact line has no code, moves to the next
	 * line with code in the same file (DAP allows this). Empty result = no code.
	 */
	public function resolveLine(file:String, line:Int):Array<{fidx:Int, op:Int, line:Int}> {
		var fileMatches = matchingFileIndexes(file);
		if (!fileMatches.keys().hasNext()) {
			return [];
		}

		// collect the smallest line >= requested that has code, per the whole file
		var effectiveLine = -1;
		for (fidx in 0...data.functions.length) {
			var debug = data.functions[fidx].debug;
			var op = 0;
			while (op < data.functions[fidx].ops.length) {
				var f = debug[op << 1];
				var l = debug[(op << 1) + 1];
				if (fileMatches.exists(f) && l >= line) {
					if (effectiveLine < 0 || l < effectiveLine) {
						effectiveLine = l;
					}
				}
				op++;
			}
		}
		if (effectiveLine < 0) {
			return [];
		}

		var result:Array<{fidx:Int, op:Int, line:Int}> = [];
		for (fidx in 0...data.functions.length) {
			var debug = data.functions[fidx].debug;
			var ops = data.functions[fidx].ops.length;
			var op = 0;
			while (op < ops) {
				var f = debug[op << 1];
				var l = debug[(op << 1) + 1];
				if (fileMatches.exists(f) && l == effectiveLine) {
					result.push({fidx: fidx, op: op, line: effectiveLine});
					break; // first op of this line in this function is enough
				}
				op++;
			}
		}
		return result;
	}

	/** Reverse mapping: bytecode location -> source (file, line). */
	public function lookup(fidx:Int, op:Int):Null<{file:String, line:Int}> {
		if (fidx < 0 || fidx >= data.functions.length) {
			return null;
		}
		var debug = data.functions[fidx].debug;
		if (debug == null || (op << 1) + 1 >= debug.length) {
			return null;
		}
		var fileIndex = debug[op << 1];
		var line = debug[(op << 1) + 1];
		var file = (fileIndex >= 0 && fileIndex < data.debugFiles.length) ? data.debugFiles[fileIndex] : null;
		return {file: file, line: line};
	}

	/** Best-effort display name ("Class.method") for a stack frame, else "fn@<findex>". */
	public function functionName(fidx:Int):String {
		if (fidx < 0 || fidx >= data.functions.length) {
			return "?";
		}
		var findex = data.functions[fidx].findex;
		var name = namesByFindex.get(findex);
		return name != null ? name : 'fn@$findex';
	}

	function matchingFileIndexes(requested:String):Map<Int, Bool> {
		var normalizedRequest = normalize(requested);
		var basename = baseName(normalizedRequest);
		var result = new Map<Int, Bool>();
		for (i in 0...data.debugFiles.length) {
			var stored = normalize(data.debugFiles[i]);
			if (baseName(stored) != basename) {
				continue;
			}
			if (stored == normalizedRequest
				|| StringTools.endsWith(normalizedRequest, "/" + stored)
				|| StringTools.endsWith(stored, "/" + normalizedRequest)
				|| StringTools.endsWith(normalizedRequest, stored)
				|| StringTools.endsWith(stored, normalizedRequest)) {
				result.set(i, true);
			}
		}
		return result;
	}

	function normalize(path:String):String {
		var p = StringTools.replace(path, "\\", "/");
		return isWindows ? p.toLowerCase() : p;
	}

	function baseName(path:String):String {
		var slash = path.lastIndexOf("/");
		return slash < 0 ? path : path.substr(slash + 1);
	}

	function buildFunctionIndex():Map<Int, Int> {
		var byFindex = new Map<Int, Int>();
		for (i in 0...data.functions.length) {
			byFindex.set(data.functions[i].findex, i);
		}
		return byFindex;
	}

	// A class's static methods are stored as bindings of its "$Class" statics
	// container (binding.mid = the method's findex). Mapping each such findex back
	// to that container lets a stopped frame find the class whose statics to show.
	function buildStaticsIndex():Map<Int, ObjPrototype> {
		var byFindex = new Map<Int, ObjPrototype>();
		for (type in data.types) {
			switch (type) {
				case HObj(proto) | HStruct(proto):
					for (binding in proto.bindings) {
						if (binding.mid >= 0) {
							byFindex.set(binding.mid, proto);
						}
					}
				default:
			}
		}
		return byFindex;
	}

	// A statics container's singleton is itself a global whose type is that
	// container, so scan the globals for each HObj/HStruct type name.
	function buildGlobalTypeIndex():Map<String, Int> {
		var byName = new Map<String, Int>();
		for (g in 0...data.globals.length) {
			switch (data.globals[g]) {
				case HObj(proto) | HStruct(proto):
					if (!byName.exists(proto.name)) {
						byName.set(proto.name, g);
					}
				default:
			}
		}
		return byName;
	}

	function buildTypeNameIndex():Map<String, HLType> {
		var byName = new Map<String, HLType>();
		for (type in data.types) {
			switch (type) {
				case HObj(proto) | HStruct(proto):
					if (!byName.exists(proto.name)) {
						byName.set(proto.name, type);
					}
				case HEnum(proto):
					if (proto.name != null && !byName.exists(proto.name)) {
						byName.set(proto.name, type);
					}
				default:
			}
		}
		return byName;
	}

	function buildNames():Map<Int, String> {
		var names = new Map<Int, String>();
		for (type in data.types) {
			switch (type) {
				case HObj(proto) | HStruct(proto):
					var className = displayClassName(proto.name);
					// instance methods live in the virtual table
					for (entry in proto.proto) {
						if (entry.findex >= 0) {
							names.set(entry.findex, className + "." + entry.name);
						}
					}
					// static methods live as field bindings; the binding's field id is an
					// absolute index that counts inherited fields first, so offset by the
					// super-class field count to index into this type's own fields.
					var inherited = fieldCount(proto.tsuper);
					for (binding in proto.bindings) {
						var ownIndex = binding.fid - inherited;
						if (binding.mid >= 0 && ownIndex >= 0 && ownIndex < proto.fields.length) {
							names.set(binding.mid, className + "." + proto.fields[ownIndex].name);
						}
					}
				default:
			}
		}
		return names;
	}

	// Haxe names the static container "$Main"; strip the leading $ for display.
	function displayClassName(name:String):String {
		return (name != null && StringTools.startsWith(name, "$")) ? name.substr(1) : name;
	}

	// Total number of fields contributed by a type's super-class chain.
	function fieldCount(type:HLType):Int {
		if (type == null) {
			return 0;
		}
		return switch (type) {
			case HObj(proto) | HStruct(proto): fieldCount(proto.tsuper) + proto.fields.length;
			default: 0;
		}
	}
}

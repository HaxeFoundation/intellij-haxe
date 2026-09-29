package ijhaxe.debug.module;
import format.hl.Reader;
import format.hl.Tools;
import haxe.io.BytesInput;
import ijhaxe.dap.protocol.Source;

import ijhaxe.debug.HostPlatform;
import ijhaxe.debug.DebugError;

import format.hl.Data;
import format.hl.Data.HLType;
import format.hl.Data.Opcode;

/**
	Reads the debug tables of a .hl file (through the `format` haxelib) and maps
	between source positions (file, line) and bytecode positions (function
	index, opcode).

	Two indexes identify a function. A `findex` is the bytecode's own function
	id, which calls and bindings refer to. An `fidx` is the position in the
	module's function array, which is also the order of the handshake's
	function table, so an (fidx, op) from here goes straight to
	JitInfo.addressOf. Natives have a findex but no fidx.
**/
class ModuleDebugInfo {
	final data:Data;
	final isWindows:Bool;

	// findex -> "Class.method" display name
	final namesByFindex:Map<Int, String>;

	// "Class.method" -> findex, for calling runtime helpers such as
	// "String.fromUTF8" through the eval-call machinery
	final findexByName:Map<String, Int>;

	final functionIndexByFindex:Map<Int, Int>;

	// findex -> the "$Class" statics container of the class that owns the function
	final staticsProtoByFindex:Map<Int, ObjPrototype>;

	// statics container type name (e.g. "$Config") -> the index of the global
	// whose slot holds the class's statics singleton
	final globalIndexByTypeName:Map<String, Int>;

	// type name -> module HLType, for resolving runtime hl_type names
	final typesByName:Map<String, HLType>;

	public function new(hlFilePath:String) {
		var bytes = sys.io.File.getBytes(hlFilePath);
		try {
			data = new Reader().read(new BytesInput(bytes));
		} catch (e:Dynamic) {
			// The format lib's "HL Version 6 is not supported" reads like a HashLink
			// runtime problem. It is the bytecode format written by the Haxe compiler.
			var reason = Std.string(e);
			if (StringTools.contains(reason, "Version")) {
				reason += ' - this is the bytecode format version stored in the .hl file, not the HashLink runtime version;'
					+ ' supported formats are 2-5, produced by Haxe 4.x';
			}
			throw new DebugError('Cannot parse "$hlFilePath" as HashLink bytecode: ' + reason);
		}
		if (!data.flags.has(HasDebug)) {
			throw new DebugError('The program "$hlFilePath" was compiled without debug info; recompile with -debug');
		}
		isWindows = HostPlatform.IS_WINDOWS;
		namesByFindex = buildNames();
		findexByName = [for (findex => name in namesByFindex) name => findex];
		functionIndexByFindex = buildFunctionIndex();
		staticsProtoByFindex = buildStaticsIndex();
		globalIndexByTypeName = buildGlobalTypeIndex();
		typesByName = buildTypeNameIndex();
	}

	/**
		Module type by its runtime name (class, struct or enum), or null.
	**/
	public function typeByName(name:String):Null<HLType> {
		return typesByName.get(name);
	}

	/**
		True when `name` is the full name (`pkg.Cls`) or the simple name (`Cls`)
		of a module type. Without imports to consult, this tells a real type in
		an `is` check from a typo.
	**/
	public function typeNameExists(name:String):Bool {
		if (typesByName.exists(name)) {
			return true;
		}
		for (full in typesByName.keys()) {
			if (simpleName(full) == name) {
				return true;
			}
		}
		return false;
	}

	static inline function simpleName(full:String):String {
		var dot = full.lastIndexOf(".");
		return dot < 0 ? full : full.substr(dot + 1);
	}

	// Dynamic objects store field names as hl_hash values. Every such name is in
	// the module's string table, so hashing all strings once gives the reverse
	// mapping (as hld Module.reverseHash does).
	var reversedHashes:Null<Map<Int, String>> = null;

	/**
		The module string with the given hl_hash, or null.
	**/
	public function reverseHash(hash:Int):Null<String> {
		if (reversedHashes == null) {
			reversedHashes = new Map();
			for (s in data.strings) {
				reversedHashes.set(Tools.hash(s), s);
			}
		}
		return reversedHashes.get(hash);
	}

	/**
		The types of the module's globals, in index order.
	**/
	public function globals():Array<HLType> {
		return data.globals;
	}

	/**
		The statics container ("$Class") of the class that owns the function at
		`fidx`: the statics to show while stopped in that function. Null when
		there is no such container or `fidx` is invalid.
	**/
	public function staticsProtoForFunction(fidx:Int):Null<ObjPrototype> {
		if (fidx < 0 || fidx >= data.functions.length) {
			return null;
		}
		return staticsProtoByFindex.get(data.functions[fidx].findex);
	}

	/**
		The index of the global whose slot holds the statics singleton of a
		"$Class" container, or -1 if none. The singleton's type is the container
		itself, so it is the global of that type.
	**/
	public function staticsGlobalIndex(proto:ObjPrototype):Int {
		var index = globalIndexByTypeName.get(proto.name);
		return index != null ? index : -1;
	}

	public function functionCount():Int {
		return data.functions.length;
	}

	public function opCount(fidx:Int):Int {
		return data.functions[fidx].ops.length;
	}

	/**
		The decoded opcodes of a function.
	**/
	public function opcodes(fidx:Int):Array<Opcode> {
		return data.functions[fidx].ops;
	}

	/**
		The function's type (an HFun), for reading its argument count/types.
	**/
	public function functionType(fidx:Int):HLType {
		return data.functions[fidx].t;
	}

	/**
		The function's register types (arguments first, then locals).
	**/
	public function registers(fidx:Int):Array<HLType> {
		return data.functions[fidx].regs;
	}

	/**
		The debug "assigns" table: each entry names a variable (a string index)
		and the opcode position where it is assigned. Arguments have a negative
		position.
	**/
	public function assignsOf(fidx:Int):Array<{varName:Int, position:Int}> {
		return data.functions[fidx].assigns;
	}

	public function stringAt(index:Int):String {
		return (index >= 0 && index < data.strings.length) ? data.strings[index] : "?";
	}

	/**
		Number of arguments, including the implicit `this` of an instance method.
	**/
	public function argCount(fidx:Int):Int {
		return switch (data.functions[fidx].t) {
			case HFun(f): f.args.length;
			default: 0;
		}
	}

	/**
		The register written by the opcode at `op`, or -1 if it writes none.
	**/
	public function destinationRegister(fidx:Int, op:Int):Int {
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

	/**
		Source line of a single opcode, or 0 when unknown.
	**/
	public function lineOf(fidx:Int, op:Int):Int {
		if (fidx < 0 || fidx >= data.functions.length) {
			return 0;
		}
		var debug = data.functions[fidx].debug;
		var index = (op << 1) + 1;
		return (debug != null && index < debug.length) ? debug[index] : 0;
	}

	/**
		The register holding the closure that an OCallClosure at `op` calls, or
		-1 when the op is not a closure call. The callee is only known at run
		time: step-into reads the vclosure pointer from this register's frame
		slot to find the entry address.
	**/
	public function closureCallRegister(fidx:Int, op:Int):Int {
		if (fidx < 0 || fidx >= data.functions.length) {
			return -1;
		}
		var ops = data.functions[fidx].ops;
		if (op < 0 || op >= ops.length) {
			return -1;
		}
		return switch (ops[op]) {
			case OCallClosure(_, fun, _): fun;
			default: -1;
		}
	}

	/**
		The fidx of the function that a static call (OCall0..4, OCallN) at `op`
		calls. Returns -1 for other opcodes and for method and closure calls,
		whose target is not known statically; step-in then behaves like
		step-over.
	**/
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
		Resolves a source line to bytecode locations, one per function with code
		on exactly that line. An empty result means the line has no code,
		usually because the binary is stale; the breakpoint is then shown as
		unverified.
	**/
	public function resolveLine(file:String, line:Int):Array<{fidx:Int, op:Int, line:Int}> {
		var fileMatches = matchingFileIndexes(file);
		if (!fileMatches.keys().hasNext()) {
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
				if (fileMatches.exists(f) && l == line) {
					result.push({fidx: fidx, op: op, line: line});
					break; // first op of this line in this function is enough
				}
				op++;
			}
		}
		return result;
	}

	/**
		The source position (file, line) of a bytecode location.
	**/
	public function sourceLineAt(fidx:Int, op:Int):Null<SourceLine> {
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
		// the Haxe compiler records "?" as the file of generated code without a position
		if (file == "?" || file == "") {
			file = null;
		}
		return {file: file, line: line};
	}

	/**
		The "Class.method" display name of a function by its findex, or null.
	**/
	public function functionNameByFindex(findex:Int):Null<String> {
		return namesByFindex.get(findex);
	}

	/**
		The fidx of a function by its qualified name ("String.fromUTF8"), or -1
		when the name is unknown or names a native, which has no jitted body.
	**/
	public function functionIndexByName(name:String):Int {
		var findex = findexByName.get(name);
		if (findex == null) {
			return -1;
		}
		var fidx = functionIndexByFindex.get(findex);
		return fidx == null ? -1 : fidx;
	}

	/**
		The fidx of a findex (for example from a method entry), which is what
		functionType and JitInfo.functionEntry expect. Returns -1 for a native,
		which has no jitted body, or an unknown findex.
	**/
	public function functionArrayIndex(findex:Int):Int {
		var fidx = functionIndexByFindex.get(findex);
		return fidx == null ? -1 : fidx;
	}

	/**
		The findex of an imported C native by its name (e.g. "alloc_bytes"), or
		-1 when the program does not import it. A native has no jitted body, so
		this findex cannot be called; NativeResolver uses it to find a jitted
		call site of the native.
	**/
	public function nativeFindexByName(name:String):Int {
		for (n in data.natives) {
			if (n.name == name) {
				return n.findex;
			}
		}
		return -1;
	}

	/**
		The display name of the function at `fidx` ("Class.method"), or "fn@<findex>" when it has none.
	**/
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
			var sameFile = stored == normalizedRequest
				|| StringTools.endsWith(normalizedRequest, "/" + stored)
				|| StringTools.endsWith(stored, "/" + normalizedRequest)
				|| StringTools.endsWith(normalizedRequest, stored)
				|| StringTools.endsWith(stored, normalizedRequest);

			if (sameFile) {
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

	/**
		The type name of a class's statics container. HashLink keeps a class's
		static fields and methods on a separate container type, named by
		putting `$` in front of the LAST path segment: `pkg.Cls` becomes
		`pkg.$Cls`, not `$pkg.Cls`.
	**/
	public static function staticsContainerName(className:String):String {
		var dot = className.lastIndexOf(".");
		return dot < 0
			? "$" + className
			: className.substr(0, dot + 1) + "$" + className.substr(dot + 1);
	}

	/** Whether a type name is a statics container name (its last segment starts with `$`). */
	public static function isStaticsContainerName(typeName:String):Bool {
		var dot = typeName.lastIndexOf(".");
		return dot + 1 < typeName.length && typeName.charCodeAt(dot + 1) == "$".code;
	}

	// Maps each function's findex to the statics container of the class that
	// owns it. Static methods are bindings of the container itself
	// (binding.mid). Instance methods live in the virtual table of the instance
	// type (proto.proto), whose container is found by name.
	function buildStaticsIndex():Map<Int, ObjPrototype> {
		var containersByName = new Map<String, ObjPrototype>();
		for (type in data.types) {
			switch (type) {
				case HObj(proto) | HStruct(proto) if (isStaticsContainerName(proto.name)):
					containersByName.set(proto.name, proto);
				default:
			}
		}

		var byFindex = new Map<Int, ObjPrototype>();
		for (type in data.types) {
			switch (type) {
				case HObj(proto) | HStruct(proto):
					for (binding in proto.bindings) {
						if (binding.mid >= 0) {
							byFindex.set(binding.mid, proto);
						}
					}
					if (!isStaticsContainerName(proto.name)) {
						var container = containersByName.get(staticsContainerName(proto.name));
						if (container != null) {
							for (entry in proto.proto) {
								if (entry.findex >= 0 && !byFindex.exists(entry.findex)) {
									byFindex.set(entry.findex, container);
								}
							}
						}
					}
				default:
			}
		}
		return byFindex;
	}

	// The statics singleton is the global whose type is the container, so the
	// first global of each object type name gives its index.
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
							names.set(entry.findex, '$className.${entry.name}');
						}
					}
					// static methods are field bindings; a binding's field id is an
					// absolute index that counts inherited fields first
					var inherited = fieldCount(proto.tsuper);
					for (binding in proto.bindings) {
						if (binding.mid < 0) {
							continue;
						}
						var ownIndex = binding.fid - inherited;
						if (ownIndex >= 0 && ownIndex < proto.fields.length) {
							names.set(binding.mid, '$className.${proto.fields[ownIndex].name}');
						} else if (fieldNameAtAbsoluteIndex(proto, binding.fid) == "__constructor__") {
							// the constructor is bound on the statics container at the
							// __constructor__ field inherited from hl.Class
							names.set(binding.mid, '$className.new');
						}
					}
				default:
			}
		}
		return names;
	}

	// The field name at an absolute field index, which counts the superclass
	// chain's fields first; null when out of range.
	function fieldNameAtAbsoluteIndex(proto:ObjPrototype, fid:Int):Null<String> {
		var chain:Array<ObjPrototype> = [];
		var cur:Null<HLType> = HObj(proto);
		while (cur != null) {
			switch (cur) {
				case HObj(p) | HStruct(p):
					chain.unshift(p);
					cur = p.tsuper;
				default:
					cur = null;
			}
		}
		var index = 0;
		for (link in chain) {
			for (f in link.fields) {
				if (index == fid) {
					return f.name;
				}
				index++;
			}
		}
		return null;
	}

	// The class name without a statics container's `$` ("Pkg.$Cls" -> "Pkg.Cls"),
	// so display and name lookup both use the real class name.
	function displayClassName(name:String):String {
		if (name == null) {
			return name;
		}
		var dot = name.lastIndexOf(".");
		if (dot >= 0) {
			return (dot + 1 < name.length && name.charCodeAt(dot + 1) == "$".code)
				? name.substr(0, dot + 1) + name.substr(dot + 2)
				: name;
		}
		return StringTools.startsWith(name, "$") ? name.substr(1) : name;
	}

	// Total number of fields of `type` and its superclasses.
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

/**
	A source position: the file path and 1-based line number.
**/
typedef SourceLine = {file:String, line:Int}

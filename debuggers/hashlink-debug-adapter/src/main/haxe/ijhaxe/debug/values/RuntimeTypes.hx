package ijhaxe.debug.values;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.target.MemoryReader;

import format.hl.Data.HLType;
import haxe.Int64;

/**
	Resolves a runtime `hl_type*` (found in value headers: object/vdynamic/venum
	headers, a varray's element type) back to a module HLType.

	hl_type layout: kind i32 @ +0, kind-specific data pointer @ +ptr.
	- primitive kinds map directly (format HLType constructor order matches the
	  C hl_type_kind indices exactly);
	- HOBJ/HSTRUCT: data -> hl_type_obj { i32 nfields/nproto/nbindings, then the
	  uchar* name at Align.objTypeName } -> resolve the UCS-2 name against the
	  module's types;
	- HENUM: data -> hl_type_enum { uchar* name @ +0 } -> resolve by name;
	- HNULL/HREF: data is the wrapped hl_type*.
	All offsets come from the `ijhaxe.debug.layout.Align` arch descriptor. Anything
	unknown or unresolvable returns null; callers fall back to the static
	(bytecode) type.
**/
class RuntimeTypes {
	static inline var KFUN = 10;
	static inline var KOBJ = 11;
	static inline var KARRAY = 12;
	static inline var KTYPE = 13;
	static inline var KREF = 14;
	static inline var KDYNOBJ = 16;
	static inline var KABSTRACT = 17;
	static inline var KENUM = 18;
	static inline var KNULL = 19;
	static inline var KSTRUCT = 21;
	static inline var KGUID = 23;
	static inline var MAX_NAME_CHARS = 256;
	static inline var MAX_FUN_ARGS = 32;

	static final PRIMITIVES:Array<HLType> = [HVoid, HUi8, HUi16, HI32, HI64, HF32, HF64, HBool, HBytes, HDyn];

	final mem:MemoryReader;
	final align:Align;
	final resolveName:String->Null<HLType>;
	final cache:Map<String, HLType> = new Map(); // keyed by the hl_type* address
	// in-flight resolutions: a (theoretical) self-referential signature must
	// not recurse forever through its own arg/ret types
	final resolving:Map<String, Bool> = new Map();

	public function new(mem:MemoryReader, align:Align, resolveName:String->Null<HLType>) {
		this.mem = mem;
		this.align = align;
		this.resolveName = resolveName;
	}

	/**
		The module HLType for the runtime type at `typePtr`, or null when unknown.
	**/
	public function typeAt(typePtr:Pointer):Null<HLType> {
		if (typePtr.isNull()) {
			return null;
		}
		var key = Int64.toStr(typePtr);
		var cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		if (resolving.exists(key)) {
			return null; // cycle: let the outer resolution degrade this leg
		}
		resolving.set(key, true);
		var resolved = resolve(typePtr);
		resolving.remove(key);
		if (resolved != null) {
			cache.set(key, resolved);
		}
		return resolved;
	}

	function resolve(typePtr:Pointer):Null<HLType> {
		var kind = mem.readI32(typePtr);
		if (kind >= 0 && kind < PRIMITIVES.length) {
			return PRIMITIVES[kind];
		}
		return switch (kind) {
			case KARRAY: HArray;
			case KTYPE: HType;
			case KDYNOBJ: HDynObj;
			case KABSTRACT:
				// for abstracts the hl_type's data pointer IS the uchar* name
				var name = readName(dataPtr(typePtr));
				name == "" ? null : HAbstract(name);
			case KGUID:
				// the format lib has no HGUID constructor; a GUID is stored as an
				// i64, so display it as its raw Int64 value
				HI64;
			case KOBJ, KSTRUCT:
				var data = dataPtr(typePtr);
				data.isNull() ? null : resolveName(readName(offsetName(data)));
			case KENUM:
				var data = dataPtr(typePtr);
				data.isNull() ? null : resolveName(readName(mem.readPointer(data)));
			case KNULL, KREF:
				var inner = typeAt(dataPtr(typePtr));
				inner == null ? null : (kind == KNULL ? HNull(inner) : HRef(inner));
			case KFUN:
				resolveFun(typePtr);
			default:
				null;
		}
	}

	// hl_type_fun: hl_type **args @ +0, hl_type *ret @ +ptr, i32 nargs @ +ptr*2.
	// Reconstructing the signature makes a closure reached through a DYNAMIC
	// slot (an Array<()->Int> element, a Dynamic local) render exactly like a
	// statically typed one — "function Holder.grab" with its "() -> Int"
	// signature, via the readClosure path — instead of a bare "Dynamic" leaf.
	// Unresolvable pieces degrade to HDyn; the closure still renders, just
	// with a looser signature.
	function resolveFun(typePtr:Pointer):HLType {
		var data = dataPtr(typePtr);
		if (data.isNull()) {
			return HFun(null);
		}
		var nargs = mem.readI32(Int64.add(data, Int64.ofInt(align.ptr * 2)));
		if (nargs < 0 || nargs > MAX_FUN_ARGS) {
			return HFun(null); // a corrupt count must never drive a huge read
		}
		var argsPtr = mem.readPointer(data);
		var args:Array<HLType> = [];
		for (i in 0...nargs) {
			var arg = argsPtr.isNull() ? null
				: typeAt(mem.readPointer(Int64.add(argsPtr, Int64.ofInt(i * align.ptr))));
			args.push(arg != null ? arg : HDyn);
		}
		var ret = typeAt(mem.readPointer(Int64.add(data, Int64.ofInt(align.ptr))));
		return HFun({args: args, ret: ret != null ? ret : HDyn});
	}

	function dataPtr(typePtr:Pointer):Pointer {
		return mem.readPointer(Int64.add(typePtr, Int64.ofInt(align.ptr)));
	}

	// name pointer for HOBJ/HSTRUCT lives inside hl_type_obj (Align.objTypeName)
	function offsetName(objData:Pointer):Pointer {
		return mem.readPointer(Int64.add(objData, Int64.ofInt(align.objTypeName)));
	}

	// null-terminated UCS-2, capped; a failed/zeroed read yields "" which simply
	// fails the name lookup and falls back to the static type
	function readName(namePtr:Pointer):String {
		if (namePtr.isNull()) {
			return "";
		}
		var buf = new StringBuf();
		var raw = mem.read(namePtr, MAX_NAME_CHARS * 2);
		for (i in 0...MAX_NAME_CHARS) {
			var c = raw.getUInt16(i * 2);
			if (c == 0) {
				break;
			}
			buf.addChar(c);
		}
		return buf.toString();
	}
}

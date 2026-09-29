package ijhaxe.debug.values;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.target.MemoryReader;

import format.hl.Data.HLType;
import haxe.Int64;

/**
	Resolves a runtime `hl_type*`, as found in the headers of objects,
	vdynamics and venums or as a varray's element type, back to a module HLType.

	An hl_type holds its kind (i32 @ +0) and a pointer to kind-specific data
	(@ +ptr):
	 - Primitive kinds map directly: the order of format's HLType constructors
	   matches the C hl_type_kind values.
	 - HOBJ and HSTRUCT: the data is an hl_type_obj, whose UCS-2 name (at
	   Align.objTypeName) is looked up among the module's types.
	 - HENUM: the data is an hl_type_enum with the name @ +0, looked up the same way.
	 - HNULL and HREF: the data is the wrapped hl_type*.
	Unknown or unresolvable types return null, and callers keep the static
	type from the bytecode.
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
	// types being resolved right now, so a function type that refers to itself
	// through its argument or return types cannot recurse forever
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
			return null; // a cycle: the outer resolution treats this part as unresolved
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
				// the format lib has no HGUID; a GUID is stored as an i64, so it shows as an Int64
				HI64;
			case KOBJ, KSTRUCT:
				var data = dataPtr(typePtr);
				data.isNull() ? null : resolveName(readName(objectNamePointer(data)));
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
	// With the signature, a closure held in a Dynamic slot (an Array<() -> Int>
	// element, a Dynamic local) displays like a statically typed one, e.g.
	// "function Holder.grab" of type "() -> Int", instead of a bare "Dynamic".
	// An argument or return type that cannot be resolved becomes HDyn.
	function resolveFun(typePtr:Pointer):HLType {
		var data = dataPtr(typePtr);
		if (data.isNull()) {
			return HFun(null);
		}
		var nargs = mem.readI32(Int64.add(data, Int64.ofInt(align.ptr * 2)));
		if (nargs < 0 || nargs > MAX_FUN_ARGS) {
			return HFun(null); // a corrupt count must not cause a huge read
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

	// the name pointer inside an hl_type_obj
	function objectNamePointer(objData:Pointer):Pointer {
		return mem.readPointer(Int64.add(objData, Int64.ofInt(align.objTypeName)));
	}

	// A NUL-terminated UCS-2 name, capped at MAX_NAME_CHARS. A null pointer or
	// zeroed memory yields "", which matches no type, so the caller keeps the
	// static type.
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

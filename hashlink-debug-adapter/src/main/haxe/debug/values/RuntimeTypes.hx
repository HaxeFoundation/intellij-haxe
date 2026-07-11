package debug.values;

import debug.Pointer;
import debug.target.MemoryReader;

import format.hl.Data.HLType;
import haxe.Int64;

/**
 * Resolves a runtime `hl_type*` (found in value headers: object/vdynamic/venum
 * headers, a varray's element type) back to a module HLType.
 *
 * hl_type layout (64-bit): kind i32 @ +0, kind-specific data pointer @ +8.
 * - primitive kinds map directly (format HLType constructor order matches the
 *   C hl_type_kind indices exactly);
 * - HOBJ/HSTRUCT: data -> hl_type_obj { i32 nfields/nproto/nbindings, pad,
 *   uchar* name @ +16 } -> resolve the UCS-2 name against the module's types;
 * - HENUM: data -> hl_type_enum { uchar* name @ +0 } -> resolve by name;
 * - HNULL/HREF: data is the wrapped hl_type*.
 * Anything unknown or unresolvable returns null; callers fall back to the
 * static (bytecode) type.
 */
class RuntimeTypes {
	static inline var KFUN = 10;
	static inline var KOBJ = 11;
	static inline var KARRAY = 12;
	static inline var KTYPE = 13;
	static inline var KREF = 14;
	static inline var KDYNOBJ = 16;
	static inline var KENUM = 18;
	static inline var KNULL = 19;
	static inline var KSTRUCT = 21;
	static inline var OBJ_NAME_OFFSET = 16; // hl_type_obj: 3 x i32 + pad
	static inline var MAX_NAME_CHARS = 256;

	static final PRIMITIVES:Array<HLType> = [HVoid, HUi8, HUi16, HI32, HI64, HF32, HF64, HBool, HBytes, HDyn];

	final mem:MemoryReader;
	final resolveName:String->Null<HLType>;
	final cache:Map<String, HLType> = new Map(); // keyed by the hl_type* address

	public function new(mem:MemoryReader, resolveName:String->Null<HLType>) {
		this.mem = mem;
		this.resolveName = resolveName;
	}

	/** The module HLType for the runtime type at `typePtr`, or null when unknown. */
	public function typeAt(typePtr:Pointer):Null<HLType> {
		if (isNull(typePtr)) {
			return null;
		}
		var key = Int64.toStr(typePtr);
		var cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		var resolved = resolve(typePtr);
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
			case KOBJ, KSTRUCT:
				var data = dataPtr(typePtr);
				isNull(data) ? null : resolveName(readName(offsetName(data)));
			case KENUM:
				var data = dataPtr(typePtr);
				isNull(data) ? null : resolveName(readName(mem.readPointer(data)));
			case KNULL, KREF:
				var inner = typeAt(dataPtr(typePtr));
				inner == null ? null : (kind == KNULL ? HNull(inner) : HRef(inner));
			default:
				null;
		}
	}

	function dataPtr(typePtr:Pointer):Pointer {
		return mem.readPointer(Int64.add(typePtr, Int64.ofInt(mem.pointerSize)));
	}

	// name pointer for HOBJ/HSTRUCT lives inside hl_type_obj
	function offsetName(objData:Pointer):Pointer {
		return mem.readPointer(Int64.add(objData, Int64.ofInt(OBJ_NAME_OFFSET)));
	}

	// null-terminated UCS-2, capped; a failed/zeroed read yields "" which simply
	// fails the name lookup and falls back to the static type
	function readName(namePtr:Pointer):String {
		if (isNull(namePtr)) {
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

	static inline function isNull(p:Pointer):Bool {
		return Int64.eq(p, Int64.ofInt(0));
	}
}

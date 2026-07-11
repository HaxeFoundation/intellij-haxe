package debug.values;

import debug.Pointer;
import debug.layout.Align;
import debug.layout.EnumLayout;
import debug.target.MemoryReader;

import format.hl.Data.HLType;
import haxe.Int64;

/**
 * Decodes a value at a memory address given its HLType, producing a display
 * string + type label (+ a reference for expandable values). Milestone 4 step 1
 * handles primitives, strings and null; other pointer types render as a raw
 * `<Type> @ 0x..` until object/array expansion (step 2) sets `referenceAllocator`.
 */
class ValueReader {
	final mem:MemoryReader;
	final align:Align;

	// Step 2 sets this to allocate a variablesReference for an expandable value.
	public var referenceAllocator:Null<(Pointer, HLType) -> Int> = null;
	// Resolves runtime hl_type* headers (vdynamic payloads, actual object classes).
	public var runtimeTypes:Null<RuntimeTypes> = null;
	// Resolves a jitted code address to a function display name (closures).
	public var functionNameResolver:Null<Pointer->Null<String>> = null;
	// Constructor-param offsets, for inline enum display and expansion.
	public var enumLayout:Null<EnumLayout> = null;

	public function new(mem:MemoryReader, align:Align) {
		this.mem = mem;
		this.align = align;
	}

	public function read(address:Pointer, t:HLType):DecodedValue {
		return switch (t) {
			case HVoid: leaf("void", "Void");
			case HUi8: leaf(Std.string(mem.readU8(address)), "UInt");
			case HUi16: leaf(Std.string(mem.readU16(address)), "UInt");
			case HI32: leaf(Std.string(mem.readI32(address)), "Int");
			case HI64: leaf(Int64.toStr(mem.readI64(address)), "Int64");
			case HF32: leaf(Std.string(mem.readF32(address)), "Float");
			case HF64: leaf(Std.string(mem.readF64(address)), "Float");
			case HBool: leaf(mem.readU8(address) != 0 ? "true" : "false", "Bool");
			default: readPointerValue(address, t);
		}
	}

	function readPointerValue(address:Pointer, t:HLType):DecodedValue {
		var ptr = mem.readPointer(address);
		if (isNull(ptr)) {
			return leaf("null", typeName(t));
		}
		return decodePointed(ptr, t);
	}

	// Decodes a value whose pointer has already been dereferenced (`ptr` is the
	// object/box itself). Split from readPointerValue because a vdynamic resolves
	// to a pointer type without another indirection.
	function decodePointed(ptr:Pointer, t:HLType):DecodedValue {
		return switch (t) {
			case HObj(proto) if (proto != null && proto.name == "String"):
				leaf(readString(ptr), "String");
			case HObj(proto) if (proto != null && proto.name == ARRAY_DYN):
				// hl.types.ArrayDyn wraps an ArrayBase (ptr @ +8) whose length is @ +8
				arrayValue(ptr, t, arrayDynLength(ptr));
			case HObj(proto) if (proto != null && isArrayWrapper(proto.name)):
				// hl.types.ArrayBytes_*/ArrayObj both keep `length` right after the header
				arrayValue(ptr, t, mem.readI32(offset(ptr, align.ptr)));
			case HArray:
				// varray: at@+ptr, size@+ptr*2
				arrayValue(ptr, t, mem.readI32(offset(ptr, align.ptr * 2)));
			case HRef(inner):
				// a reference: the dereferenced pointer IS the address of the value
				// (captured-and-mutated closure locals are the common case)
				read(ptr, inner);
			case HNull(inner):
				// a box: the wrapped value sits right after the type header
				read(offset(ptr, align.ptr), inner);
			case HDyn:
				readDynamic(ptr);
			case HFun(_), HMethod(_):
				readClosure(ptr);
			case HEnum(proto) if (proto != null && enumLayout != null):
				readEnum(ptr, t, proto);
			case HVirtual(fields):
				readVirtual(ptr, t, fields);
			case HObj(_), HStruct(_):
				expandableOrRaw(ptr, refineObjectType(ptr, t));
			default:
				expandableOrRaw(ptr, t);
		}
	}

	// venum: constructor index @ +ptr; params inline per EnumLayout. Constructors
	// without params are leaves; with params the value previews them inline and
	// expands into one child per param.
	function readEnum(ptr:Pointer, t:HLType, proto:format.hl.Data.EnumPrototype):DecodedValue {
		var index = mem.readI32(offset(ptr, align.ptr));
		if (index < 0 || index >= proto.constructs.length) {
			return {value: typeName(t) + " @ " + hex(ptr), type: typeName(t), reference: 0};
		}
		var construct = proto.constructs[index];
		if (construct.params.length == 0) {
			return leaf(construct.name, typeName(t));
		}
		var parts:Array<String> = [];
		for (param in enumLayout.params(proto, index)) {
			parts.push(read(offset(ptr, param.offset), param.type).value);
		}
		var display = construct.name + "(" + parts.join(", ") + ")";
		var reference = referenceAllocator == null ? 0 : referenceAllocator(ptr, t);
		return {value: display, type: typeName(t), reference: reference};
	}

	// vvirtual: header t/value/next, then one indirect field pointer per field
	function readVirtual(ptr:Pointer, t:HLType, fields:Array<{name:String, t:HLType}>):DecodedValue {
		var names = [for (f in fields) f.name];
		var display = "{" + names.join(", ") + "}";
		var reference = (referenceAllocator == null || fields.length == 0) ? 0 : referenceAllocator(ptr, t);
		return {value: display, type: typeName(t), reference: reference};
	}

	// vdynamic: runtime type @ +0, payload @ +ptr. When the runtime type is itself
	// a pointer kind the vdynamic address *is* the value (no extra indirection).
	function readDynamic(ptr:Pointer):DecodedValue {
		var resolved = runtimeTypes == null ? null : runtimeTypes.typeAt(mem.readPointer(ptr));
		if (resolved == null) {
			return expandableOrRaw(ptr, HDyn);
		}
		return switch (resolved) {
			case HVoid, HUi8, HUi16, HI32, HI64, HF32, HF64, HBool:
				read(offset(ptr, align.ptr), resolved);
			case HDyn:
				expandableOrRaw(ptr, HDyn); // avoid recursing on a dyn-of-dyn
			default:
				decodePointed(ptr, resolved);
		}
	}

	// vclosure: function pointer @ +ptr (hasValue/captured value are not shown)
	function readClosure(ptr:Pointer):DecodedValue {
		var fun = mem.readPointer(offset(ptr, align.ptr));
		var name = functionNameResolver == null ? null : functionNameResolver(fun);
		return leaf(name != null ? "function " + name : "function @ " + hex(fun), "Function");
	}

	// Prefer the object's runtime class (hl_type* header @ +0) over the static
	// type so a Base-typed slot holding a Sub shows Sub's fields.
	function refineObjectType(ptr:Pointer, staticType:HLType):HLType {
		if (runtimeTypes == null) {
			return staticType;
		}
		var runtime = runtimeTypes.typeAt(mem.readPointer(ptr));
		return switch (runtime) {
			case HObj(_), HStruct(_): runtime;
			default: staticType;
		}
	}

	function arrayValue(ptr:Pointer, t:HLType, length:Int):DecodedValue {
		if (length < 0 || referenceAllocator == null) {
			return {value: typeName(t) + " @ " + hex(ptr), type: typeName(t), reference: 0};
		}
		var display = typeName(t) + "(" + length + ")";
		var reference = length == 0 ? 0 : referenceAllocator(ptr, t);
		return {value: display, type: typeName(t), reference: reference};
	}

	function expandableOrRaw(ptr:Pointer, t:HLType):DecodedValue {
		if (referenceAllocator != null && isExpandable(t)) {
			return {value: typeName(t), type: typeName(t), reference: referenceAllocator(ptr, t)};
		}
		return {value: typeName(t) + " @ " + hex(ptr), type: typeName(t), reference: 0};
	}

	function readString(strPtr:Pointer):String {
		var bytesPtr = mem.readPointer(offset(strPtr, align.ptr));
		var length = mem.readI32(offset(strPtr, align.ptr * 2));
		if (length <= 0 || isNull(bytesPtr)) {
			return "\"\"";
		}
		var raw = mem.read(bytesPtr, length * 2);
		var buf = new StringBuf();
		for (i in 0...length) {
			buf.addChar(raw.getUInt16(i * 2)); // UTF-16 code unit (BMP)
		}
		return "\"" + buf.toString() + "\"";
	}

	static inline function leaf(value:String, type:String):DecodedValue {
		return {value: value, type: type, reference: 0};
	}

	static inline function offset(p:Pointer, n:Int):Pointer {
		return Int64.add(p, Int64.ofInt(n));
	}

	static inline function isNull(p:Pointer):Bool {
		return Int64.eq(p, Int64.ofInt(0));
	}

	public static function isExpandable(t:HLType):Bool {
		return switch (t) {
			case HObj(proto): proto == null || proto.name != "String";
			case HStruct(_): true;
			case HArray: true;
			default: false;
		}
	}

	static inline var ARRAY_BYTES_PREFIX = "hl.types.ArrayBytes_";
	public static inline var ARRAY_DYN = "hl.types.ArrayDyn";

	/** True for the std Array wrappers (hl.types.ArrayBytes_* / ArrayObj / ArrayDyn). */
	public static function isArrayWrapper(name:String):Bool {
		return name != null
			&& (name == "hl.types.ArrayObj" || name == ARRAY_DYN || StringTools.startsWith(name, ARRAY_BYTES_PREFIX));
	}

	// ArrayDyn: inner ArrayBase pointer @ +ptr; the wrapper has no length field of
	// its own, the inner one (@ +ptr) is authoritative. -1 when the inner is null.
	function arrayDynLength(ptr:Pointer):Int {
		var inner = mem.readPointer(offset(ptr, align.ptr));
		return isNull(inner) ? -1 : mem.readI32(offset(inner, align.ptr));
	}

	/** Element type encoded in an hl.types.ArrayBytes_* class name, or null. */
	public static function arrayBytesElementType(name:String):Null<HLType> {
		if (name == null || !StringTools.startsWith(name, ARRAY_BYTES_PREFIX)) {
			return null;
		}
		return switch (name.substr(ARRAY_BYTES_PREFIX.length)) {
			case "Int": HI32;
			case "Float": HF64;
			case "hl_F32", "Single": HF32;
			case "hl_UI16": HUi16;
			case "hl_UI8": HUi8;
			case "hl_I64": HI64;
			default: null;
		}
	}

	public static function typeName(t:HLType):String {
		return switch (t) {
			case HVoid: "Void";
			case HUi8, HUi16, HI32: "Int";
			case HI64: "Int64";
			case HF32, HF64: "Float";
			case HBool: "Bool";
			case HBytes: "Bytes";
			case HDyn: "Dynamic";
			case HArray: "Array";
			case HObj(proto), HStruct(proto):
				proto == null ? "Object" : (isArrayWrapper(proto.name) ? "Array" : displayName(proto.name));
			case HVirtual(_): "Virtual";
			case HEnum(proto): proto != null ? proto.name : "Enum";
			case HNull(inner): typeName(inner);
			case HRef(inner): typeName(inner);
			case HFun(_), HMethod(_): "Function";
			case HAbstract(name): name;
			default: "Value";
		}
	}

	static function displayName(name:String):String {
		return (name != null && StringTools.startsWith(name, "$")) ? name.substr(1) : name;
	}

	static function hex(p:Pointer):String {
		var high = Int64.getHigh(p);
		var low = Int64.getLow(p);
		return high != 0 ? "0x" + StringTools.hex(high) + StringTools.hex(low, 8) : "0x" + StringTools.hex(low);
	}
}

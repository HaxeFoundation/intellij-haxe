package debug.values;
import format.hl.Data.EnumPrototype;
import format.hl.Data.FunPrototype;
import format.hl.Data.ObjPrototype;
import format.hl.Tools;

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
	// Resolves an hl_symbol stack-trace entry (a code return address) to a
	// "Class.method (File.hx:line)" label; null → raw pointer fallback.
	public var symbolResolver:Null<Pointer->Null<String>> = null;
	// Constructor-param offsets, for inline enum display and expansion.
	public var enumLayout:Null<EnumLayout> = null;
	// Runtime dynamic-object reader (Dynamic structures, Reflect/JSON objects).
	public var dynObjects:Null<DynObjReader> = null;
	// Native map reader for the haxe.ds map wrappers.
	public var maps:Null<MapReader> = null;
	// Pure-Haxe balanced-tree map reader (EnumValueMap / BalancedTree).
	public var treeMaps:Null<TreeMapReader> = null;

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
			case HPacked(inner):
				// a @:packed field: the field address IS the inline struct
				// (there is no pointer slot to dereference)
				expandableOrRaw(address, inner.v);
			default: readPointerValue(address, t);
		}
	}

	function readPointerValue(address:Pointer, t:HLType):DecodedValue {
		var ptr = mem.readPointer(address);
		if (ptr.isNull()) {
			return leaf("null", typeName(t));
		}
		return decodePointed(ptr, t);
	}

	/**
	 * Decodes a value whose pointer is already in hand (e.g. a function's
	 * pointer-typed return value in RAX) — no address dereference.
	 */
	public function decodeReturnedPointer(ptr:Pointer, t:HLType):DecodedValue {
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
				arrayValue(ptr, t, mem.readI32(ptr.offset(align.ptr)));
			case HArray:
				// varray: at@+ptr, size@+ptr*2
				arrayValue(ptr, t, mem.readI32(ptr.offset(align.ptr * 2)));
			case HRef(inner):
				// a reference: the dereferenced pointer IS the address of the value
				// (captured-and-mutated closure locals are the common case)
				read(ptr, inner);
			case HNull(inner):
				// a box: the wrapped value sits right after the type header
				read(ptr.offset(align.ptr), inner);
			case HDyn:
				readDynamic(ptr);
			case HFun(_), HMethod(_):
				readClosure(ptr, t);
			case HEnum(proto) if (proto != null && enumLayout != null):
				readEnum(ptr, t, proto);
			case HVirtual(fields):
				readVirtual(ptr, t, fields);
			case HDynObj if (dynObjects != null):
				readDynObj(ptr);
			case HObj(proto) if (proto != null && maps != null && mapKeyKind(proto.name) != null):
				readMapWrapper(ptr, t);
			case HObj(proto) if (proto != null && treeMaps != null && TreeMapReader.isTreeMap(proto.name)):
				readTreeMap(ptr, proto);
			case HAbstract(name) if (maps != null && nativeMapKind(name) != null):
				// the abstract value IS the native map pointer (no wrapper indirection)
				readNativeMap(ptr, nativeMapKind(name), name);
			case HAbstract("hl_symbol"):
				// a haxe.Exception.__nativeStack entry: the abstract value is a code
				// return address. Resolve it to a source location the way the call
				// stack does, instead of showing an opaque `hl_symbol @ 0x..`.
				var label = symbolResolver == null ? null : symbolResolver(ptr);
				leaf(label != null ? label : "hl_symbol @ " + hex(ptr), "StackFrame");
			case HObj(_):
				expandableOrRaw(ptr, refineObjectType(ptr, t));
			case HStruct(_):
				// structs carry no hl_type* header, so there is nothing to refine
				expandableOrRaw(ptr, t);
			default:
				expandableOrRaw(ptr, t);
		}
	}

	// haxe.ds.EnumValueMap / BalancedTree: entry count by walking the tree
	function readTreeMap(ptr:Pointer, proto:ObjPrototype):DecodedValue {
		var count = treeMaps.entryCount(ptr, proto);
		if (count < 0) {
			return {value: displayName(proto.name) + " @ " + hex(ptr), type: displayName(proto.name), reference: 0};
		}
		var reference = (count == 0 || referenceAllocator == null) ? 0 : referenceAllocator(ptr, HObj(proto));
		return {value: "Map(" + count + ")", type: "Map", reference: reference};
	}

	// a native map at `native` (a wrapper deref, or the abstract itself)
	function readNativeMap(native:Pointer, kind:MapKeyKind, abstractName:String):DecodedValue {
		var count = maps.entryCount(native);
		if (count < 0) {
			return {value: "Map @ " + hex(native), type: "Map", reference: 0};
		}
		var reference = (count == 0 || referenceAllocator == null) ? 0 : referenceAllocator(native, HAbstract(abstractName));
		return {value: "Map(" + count + ")", type: "Map", reference: reference};
	}

	/** The key layout of a native map abstract, or null when not a map native. */
	public static function nativeMapKind(name:String):Null<MapKeyKind> {
		return switch (name) {
			case "hl_bytes_map": StringKey;
			case "hl_int_map": IntKey;
			case "hl_obj_map": ObjectKey;
			case "hl_int64_map": Int64Key;
			default: null;
		}
	}

	// vdynobj: field names inline in the preview, children on expand
	function readDynObj(ptr:Pointer):DecodedValue {
		var fields = dynObjects.fields(ptr);
		if (fields.length == 0) {
			return leaf("{}", "Dynamic");
		}
		var display = "{" + [for (f in fields) f.name].join(", ") + "}";
		var reference = referenceAllocator == null ? 0 : referenceAllocator(ptr, HDynObj);
		return {value: display, type: "Dynamic", reference: reference};
	}

	// haxe.ds.StringMap/IntMap/ObjectMap: the native map lives in the wrapper's
	// first field; preview shows the live entry count
	function readMapWrapper(ptr:Pointer, t:HLType):DecodedValue {
		var native = mem.readPointer(ptr.offset(align.ptr));
		var count = maps.entryCount(native);
		if (count < 0) {
			return {value: typeName(t) + " @ " + hex(ptr), type: typeName(t), reference: 0};
		}
		var reference = (count == 0 || referenceAllocator == null) ? 0 : referenceAllocator(ptr, t);
		return {value: "Map(" + count + ")", type: "Map", reference: reference};
	}

	/** The map key layout for a wrapper class name, or null when not a map. */
	public static function mapKeyKind(name:String):Null<MapKeyKind> {
		return switch (name) {
			case "haxe.ds.StringMap": StringKey;
			case "haxe.ds.IntMap": IntKey;
			case "haxe.ds.ObjectMap": ObjectKey;
			default: null;
		}
	}

	// venum: constructor index @ +ptr; params inline per EnumLayout. Constructors
	// without params are leaves; with params the value previews them inline and
	// expands into one child per param.
	function readEnum(ptr:Pointer, t:HLType, proto:EnumPrototype):DecodedValue {
		var index = mem.readI32(ptr.offset(align.ptr));
		if (index < 0 || index >= proto.constructs.length) {
			return {value: typeName(t) + " @ " + hex(ptr), type: typeName(t), reference: 0};
		}
		var construct = proto.constructs[index];
		if (construct.params.length == 0) {
			return leaf(construct.name, typeName(t));
		}
		var parts:Array<String> = [];
		for (param in enumLayout.params(proto, index)) {
			parts.push(read(ptr.offset(param.offset), param.type).value);
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

	// vdynamic: runtime type @ +0, payload @ +ptr. Whether the vdynamic address
	// *is* the value or the value lives in the payload slot follows the VM's
	// own classification (Tools.isDynamic): objects/virtuals/enums/
	// arrays/dynobjs ARE vdynamic-compatible; primitives, abstracts, bytes,
	// refs and structs are carried in the payload.
	function readDynamic(ptr:Pointer):DecodedValue {
		var resolved = runtimeTypes == null ? null : runtimeTypes.typeAt(mem.readPointer(ptr));
		if (resolved == null) {
			return expandableOrRaw(ptr, HDyn);
		}
		return switch (resolved) {
			case HDyn:
				expandableOrRaw(ptr, HDyn); // avoid recursing on a dyn-of-dyn
			default:
				Tools.isDynamic(resolved)
					? decodePointed(ptr, resolved)
					: read(ptr.offset(align.ptr), resolved);
		}
	}

	// vclosure: function pointer @ +ptr, hasValue i32 @ +ptr*2; when bound
	// (hasValue == 1) the captured value (the bound object or the capture
	// environment) sits @ +ptr*3 and the closure expands into it
	function readClosure(ptr:Pointer, t:HLType):DecodedValue {
		var fun = mem.readPointer(ptr.offset(align.ptr));
		var name = functionNameResolver == null ? null : functionNameResolver(fun);
		var display = name != null ? "function " + name : "function @ " + hex(fun);
		var hasValue = mem.readI32(ptr.offset(align.ptr * 2));
		if (hasValue == 1 && referenceAllocator != null) {
			return {value: display, type: typeName(t), reference: referenceAllocator(ptr, t)};
		}
		return leaf(display, typeName(t));
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
		return "\"" + stringContentAt(strPtr) + "\"";
	}

	/** The UTF-16 content of a debuggee String, UNQUOTED ("" for empty). */
	public function stringContentAt(strPtr:Pointer):String {
		var bytesPtr = mem.readPointer(strPtr.offset(align.ptr));
		var length = mem.readI32(strPtr.offset(align.ptr * 2));
		if (length <= 0 || bytesPtr.isNull()) {
			return "";
		}
		var raw = mem.read(bytesPtr, length * 2);
		var buf = new StringBuf();
		for (i in 0...length) {
			buf.addChar(raw.getUInt16(i * 2)); // UTF-16 code unit (BMP)
		}
		return buf.toString();
	}

	static inline function leaf(value:String, type:String):DecodedValue {
		return {value: value, type: type, reference: 0};
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
		var inner = mem.readPointer(ptr.offset(align.ptr));
		return inner.isNull() ? -1 : mem.readI32(inner.offset(align.ptr));
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
			case HFun(fun), HMethod(fun): funSignature(fun);
			case HAbstract(name): name;
			case HPacked(inner): typeName(inner.v);
			default: "Value";
		}
	}

	static function displayName(name:String):String {
		return (name != null && StringTools.startsWith(name, "$")) ? name.substr(1) : name;
	}

	// A function/method type rendered as its Haxe signature: `(Arg, Arg) -> Ret`
	// (`() -> Void` for no args). The bytecode type table carries the parameter
	// TYPES but not their names, so the arguments are unnamed. Argument and return
	// types are named recursively, so nested function types compose.
	static function funSignature(fun:Null<FunPrototype>):String {
		if (fun == null) {
			return "Function";
		}
		var args = fun.args == null ? [] : [for (a in fun.args) typeName(a)];
		var ret = fun.ret == null ? "Unknown" : typeName(fun.ret);
		return "(" + args.join(", ") + ") -> " + ret;
	}

	public static function hex(p:Pointer):String {
		var high = p.high;
		var low = p.low;
		return high != 0 ? "0x" + StringTools.hex(high) + StringTools.hex(low, 8) : "0x" + StringTools.hex(low);
	}
}

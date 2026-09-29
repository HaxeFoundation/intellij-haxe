package ijhaxe.debug.values;
import format.hl.Data.EnumPrototype;
import format.hl.Data.FunPrototype;
import format.hl.Data.ObjPrototype;
import format.hl.Tools;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.layout.EnumLayout;
import ijhaxe.debug.target.MemoryReader;

import format.hl.Data.HLType;
import haxe.Int64;

/**
	Decodes the value at a memory address, given its HLType, into a display
	string, a type label and, for an expandable value, a reference.

	The optional readers below enable richer decoding. Without them a value
	falls back to a raw `<Type> @ 0x...`, and without `referenceAllocator` no
	value is expandable.
**/
class ValueReader {
	final mem:MemoryReader;
	final align:Align;

	// Allocates a variablesReference for an expandable value.
	public var referenceAllocator:Null<(Pointer, HLType) -> Int> = null;
	// Resolves runtime hl_type* headers: vdynamic payloads, the actual class of an object.
	public var runtimeTypes:Null<RuntimeTypes> = null;
	// Names the function at a jitted code address, for closures.
	public var functionNameResolver:Null<Pointer->Null<String>> = null;
	// Turns an hl_symbol stack-trace entry (a code return address) into
	// "Class.method (File.hx:line)"; without it the raw pointer is shown.
	public var symbolResolver:Null<Pointer->Null<String>> = null;
	// Enum constructor parameter offsets, for enum previews and children.
	public var enumLayout:Null<EnumLayout> = null;
	// Reads dynamic objects: Dynamic structures, Reflect-built and JSON objects.
	public var dynObjects:Null<DynObjReader> = null;
	// Reads the native maps inside the haxe.ds map classes.
	public var maps:Null<MapReader> = null;
	// Reads the pure-Haxe tree maps (EnumValueMap, BalancedTree).
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
				// a @:packed field stores its struct inline: no pointer to dereference
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
		Decodes a pointer value that is already in hand rather than stored at an
		address, such as a pointer returned by a call.
	**/
	public function decodeReturnedPointer(ptr:Pointer, t:HLType):DecodedValue {
		return decodePointed(ptr, t);
	}

	// Decodes the value `ptr` points to (the object or box itself). It is
	// separate from readPointerValue because a vdynamic can hold a pointer-typed
	// value at its own address, with no slot to dereference.
	function decodePointed(ptr:Pointer, t:HLType):DecodedValue {
		return switch (t) {
			case HObj(proto) if (proto != null && proto.name == "String"):
				leaf(readString(ptr), "String");

			case HObj(proto) if (proto != null && proto.name == ARRAY_DYN):
				arrayValue(ptr, t, arrayDynLength(ptr));
			case HObj(proto) if (proto != null && isArrayWrapper(proto.name)):
				// hl.types.ArrayBytes_* and ArrayObj keep `length` right after the header
				arrayValue(ptr, t, mem.readI32(ptr.offset(align.ptr)));
			case HArray:
				// varray: element type @ +ptr, size @ +ptr*2
				arrayValue(ptr, t, mem.readI32(ptr.offset(align.ptr * 2)));

			case HRef(inner):
				// the pointer is the address of the value
				read(ptr, inner);
			case HNull(inner):
				// a box shaped like a vdynamic, with the value at the payload offset
				read(ptr.offset(align.dynPayload), inner);

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
				// the abstract value is the native map itself, without a wrapper
				readNativeMap(ptr, nativeMapKind(name), name);

			case HAbstract("hl_symbol"):
				// an entry of haxe.Exception.__nativeStack: a code return address,
				// shown as a source location like a call stack frame
				var label = symbolResolver == null ? null : symbolResolver(ptr);
				leaf(label != null ? label : "hl_symbol @ " + hex(ptr), "StackFrame");

			case HObj(_):
				expandableOrRaw(ptr, refineObjectType(ptr, t));
			case HStruct(_):
				// a struct has no hl_type* header to read its runtime type from
				expandableOrRaw(ptr, t);
			default:
				expandableOrRaw(ptr, t);
		}
	}

	// haxe.ds.EnumValueMap / BalancedTree, previewed with the entry count
	function readTreeMap(ptr:Pointer, proto:ObjPrototype):DecodedValue {
		var count = treeMaps.entryCount(ptr, proto);
		if (count < 0) {
			return {value: displayName(proto.name) + " @ " + hex(ptr), type: displayName(proto.name), reference: 0};
		}
		var reference = (count == 0 || referenceAllocator == null) ? 0 : referenceAllocator(ptr, HObj(proto));
		return {value: "Map(" + count + ")", type: "Map", reference: reference};
	}

	// a native map, reached through a map class or held directly as an abstract
	function readNativeMap(native:Pointer, kind:MapKeyKind, abstractName:String):DecodedValue {
		var count = maps.entryCount(native);
		if (count < 0) {
			return {value: "Map @ " + hex(native), type: "Map", reference: 0};
		}
		var reference = (count == 0 || referenceAllocator == null) ? 0 : referenceAllocator(native, HAbstract(abstractName));
		return {value: "Map(" + count + ")", type: "Map", reference: reference};
	}

	/**
		The key layout of a native map abstract, or null when `name` is not one.
	**/
	public static function nativeMapKind(name:String):Null<MapKeyKind> {
		return switch (name) {
			case "hl_bytes_map": StringKey;
			case "hl_int_map": IntKey;
			case "hl_obj_map": ObjectKey;
			case "hl_int64_map": Int64Key;
			default: null;
		}
	}

	// vdynobj: the preview lists the field names, the children hold the values
	function readDynObj(ptr:Pointer):DecodedValue {
		var fields = dynObjects.fields(ptr);
		if (fields.length == 0) {
			return leaf("{}", "Dynamic");
		}
		var display = "{" + [for (f in fields) f.name].join(", ") + "}";
		var reference = referenceAllocator == null ? 0 : referenceAllocator(ptr, HDynObj);
		return {value: display, type: "Dynamic", reference: reference};
	}

	// haxe.ds.StringMap/IntMap/ObjectMap keep the native map in their first
	// field; the preview shows the entry count
	function readMapWrapper(ptr:Pointer, t:HLType):DecodedValue {
		var native = mem.readPointer(ptr.offset(align.ptr));
		var count = maps.entryCount(native);
		if (count < 0) {
			return {value: typeName(t) + " @ " + hex(ptr), type: typeName(t), reference: 0};
		}
		var reference = (count == 0 || referenceAllocator == null) ? 0 : referenceAllocator(ptr, t);
		return {value: "Map(" + count + ")", type: "Map", reference: reference};
	}

	/**
		The key layout of a haxe.ds map class, or null when `name` is not one.
	**/
	public static function mapKeyKind(name:String):Null<MapKeyKind> {
		return switch (name) {
			case "haxe.ds.StringMap": StringKey;
			case "haxe.ds.IntMap": IntKey;
			case "haxe.ds.ObjectMap": ObjectKey;
			default: null;
		}
	}

	// venum: constructor index @ +ptr, parameters at their EnumLayout offsets. A
	// constructor without parameters is a leaf; otherwise the preview shows the
	// parameters and the value expands into one child per parameter.
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

	// vvirtual: header t/value/next, then one slot per field
	function readVirtual(ptr:Pointer, t:HLType, fields:Array<{name:String, t:HLType}>):DecodedValue {
		// A virtual that wraps an object (a class instance seen through an
		// interface) is shown as that object. Its fields hold the real values,
		// and its class name enables source navigation. The interface's own
		// members are mostly properties and methods, which hold no data. A
		// standalone virtual (an anonymous structure, whose value is null), or
		// one whose class cannot be resolved, is shown as its member list.
		var wrapped = wrappedInstance(ptr);
		if (wrapped != null) {
			return expandableOrRaw(wrapped.ptr, wrapped.type);
		}
		var names = [for (f in fields) f.name];
		var display = "{" + names.join(", ") + "}";
		var reference = (referenceAllocator == null || fields.length == 0) ? 0 : referenceAllocator(ptr, t);
		return {value: display, type: typeName(t), reference: reference};
	}

	/**
		The object a virtual wraps (`vvirtual.value` @ +ptr), typed by its
		runtime class. Null when the virtual holds its own data (an anonymous
		structure) or the class cannot be resolved.
	**/
	public function wrappedInstance(ptr:Pointer):Null<{ptr:Pointer, type:HLType}> {
		if (runtimeTypes == null) {
			return null;
		}
		var value = mem.readPointer(ptr.offset(align.ptr));
		if (value.isNull()) {
			return null;
		}
		var runtime = runtimeTypes.typeAt(mem.readPointer(value));
		return switch (runtime) {
			case HObj(_), HStruct(_): {ptr: value, type: runtime};
			default: null;
		}
	}

	/**
		A method slot of a virtual that wraps an object. The slot holds the
		method's code pointer, whereas a data field's slot holds the field's
		address, so it is only named, never dereferenced.
	**/
	public function readMethodPointer(code:Pointer, t:HLType):DecodedValue {
		var name = functionNameResolver == null ? null : functionNameResolver(code);
		return leaf(name != null ? "function " + name : "function @ " + hex(code), typeName(t));
	}

	// vdynamic: runtime type @ +0, payload @ Align.dynPayload. The VM's own
	// classification (Tools.isDynamic) decides where the value is. Objects,
	// virtuals, enums, arrays and dynamic objects start with a type header, so
	// the vdynamic IS the value. Primitives, abstracts, bytes, refs and structs
	// are stored in the payload.
	function readDynamic(ptr:Pointer):DecodedValue {
		var resolved = runtimeTypes == null ? null : runtimeTypes.typeAt(mem.readPointer(ptr));
		if (resolved == null) {
			return expandableOrRaw(ptr, HDyn);
		}
		return switch (resolved) {
			case HDyn:
				expandableOrRaw(ptr, HDyn); // a Dynamic typed as Dynamic would recurse forever
			default:
				Tools.isDynamic(resolved)
					? decodePointed(ptr, resolved)
					: read(ptr.offset(align.dynPayload), resolved);
		}
	}

	// vclosure: function pointer @ +ptr, hasValue i32 @ +ptr*2. A bound closure
	// (hasValue == 1) holds its bound object or capture environment @ +ptr*3
	// and is expandable.
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

	// The object's runtime class (hl_type* header @ +0) wins over the static
	// type, so a slot typed Base that holds a Sub shows Sub's fields.
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

	/**
		Display text for a thrown vdynamic, such as the exception value hl_throw
		stores. The VM's own errors ("Null access .length", "Out of bounds 5/3")
		arrive as a bytes-typed dynamic holding NUL-terminated UTF-16 text, which
		is returned as is. Any other value goes through the regular decoder.
	**/
	public function previewThrownDynamic(ptr:Pointer):Null<String> {
		var resolved = runtimeTypes == null ? null : runtimeTypes.typeAt(mem.readPointer(ptr));
		if (resolved != null && resolved.match(HBytes)) {
			var text = nativeUtf16At(mem.readPointer(ptr.offset(align.dynPayload)));
			if (text != null) {
				return text;
			}
		}
		var decoded = decodeReturnedPointer(ptr, HDyn);
		return decoded == null ? null : decoded.value;
	}

	// Text of a NUL-terminated UTF-16 native string, capped because runtime
	// messages are short. It reads in small chunks, so a missing terminator or
	// an unmapped page yields the text read so far instead of failing.
	function nativeUtf16At(bytesPtr:Pointer):Null<String> {
		if (bytesPtr.isNull()) {
			return null;
		}
		var buf = new StringBuf();
		var offset = 0;
		final chunkBytes = 64;
		final maxBytes = 1024;
		while (offset < maxBytes) {
			var chunk = try mem.read(bytesPtr.offset(offset), chunkBytes) catch (e:Dynamic) null;
			if (chunk == null) {
				break;
			}
			var i = 0;
			while (i + 1 < chunkBytes) {
				var code = chunk.getUInt16(i);
				if (code == 0) {
					return buf.length > 0 ? buf.toString() : null;
				}
				buf.addChar(code); // UTF-16 code unit (BMP)
				i += 2;
			}
			offset += chunkBytes;
		}
		return buf.length > 0 ? buf.toString() : null;
	}

	function readString(strPtr:Pointer):String {
		return '"${stringContentAt(strPtr)}"';
	}

	/**
		The UTF-16 content of a debuggee String, UNQUOTED ("" for empty).
	**/
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

	/**
		True for the classes behind Haxe arrays: hl.types.ArrayBytes_*, ArrayObj and ArrayDyn.
	**/
	public static function isArrayWrapper(name:String):Bool {
		return name != null
			&& (name == "hl.types.ArrayObj" || name == ARRAY_DYN || StringTools.startsWith(name, ARRAY_BYTES_PREFIX));
	}

	// ArrayDyn has no length of its own: it wraps an ArrayBase (@ +ptr), whose
	// length is @ +ptr. -1 when there is no wrapped array.
	function arrayDynLength(ptr:Pointer):Int {
		var inner = mem.readPointer(ptr.offset(align.ptr));
		return inner.isNull() ? -1 : mem.readI32(inner.offset(align.ptr));
	}

	/**
		Element type encoded in an hl.types.ArrayBytes_* class name, or null.
	**/
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

	// The class name without a statics container's `$`, which sits on the last
	// segment (`pkg.$Cls`), not always at the start.
	static function displayName(name:String):String {
		if (name == null) {
			return name;
		}
		var dot = name.lastIndexOf(".");
		return dot + 1 < name.length && name.charCodeAt(dot + 1) == "$".code
			? name.substr(0, dot + 1) + name.substr(dot + 2)
			: name;
	}

	// A function type as a Haxe signature, `(Arg, Arg) -> Ret` or `() -> Void`.
	// The bytecode records parameter types but not their names. Nested function
	// types are rendered recursively.
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

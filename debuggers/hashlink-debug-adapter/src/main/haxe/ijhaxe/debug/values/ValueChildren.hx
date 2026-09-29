package ijhaxe.debug.values;
import format.hl.Data.EnumPrototype;
import format.hl.Data.ObjPrototype;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.layout.EnumLayout;
import ijhaxe.debug.layout.ObjectLayout;
import ijhaxe.debug.target.MemoryReader;

import format.hl.Data.HLType;
import haxe.Int64;

/**
	Lists the children of an expandable value (the target of a
	`variablesReference`): object fields, array elements, map entries, enum
	parameters, virtual and dynamic object fields, and a closure's captured
	value. Arrays list at most MAX_ELEMENTS elements followed by a "…" marker,
	because the adapter does not offer paging of variables.
**/
class ValueChildren {
	static inline var MAX_ELEMENTS = 512;

	final mem:MemoryReader;
	final align:Align;
	final reader:ValueReader;
	final objectLayout:ObjectLayout;
	public var runtimeTypes:Null<RuntimeTypes> = null;
	public var enumLayout:Null<EnumLayout> = null;
	public var dynObjects:Null<DynObjReader> = null;
	public var maps:Null<MapReader> = null;
	public var treeMaps:Null<TreeMapReader> = null;

	public function new(mem:MemoryReader, align:Align, reader:ValueReader, objectLayout:ObjectLayout) {
		this.mem = mem;
		this.align = align;
		this.reader = reader;
		this.objectLayout = objectLayout;
	}

	public function of(pointer:Pointer, t:HLType):Array<VariableInfo> {
		return switch (t) {
			case HObj(proto) if (proto != null && ValueReader.arrayBytesElementType(proto.name) != null):
				arrayBytesElements(pointer, ValueReader.arrayBytesElementType(proto.name));
			case HObj(proto) if (proto != null && proto.name == "hl.types.ArrayObj"):
				arrayObjElements(pointer);
			case HObj(proto) if (proto != null && proto.name == ValueReader.ARRAY_DYN):
				arrayDynElements(pointer, t);

			case HObj(proto) if (proto != null && maps != null && ValueReader.mapKeyKind(proto.name) != null):
				mapEntries(mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr))), ValueReader.mapKeyKind(proto.name));
			case HObj(proto) if (proto != null && treeMaps != null && TreeMapReader.isTreeMap(proto.name)):
				treeMapEntries(pointer, proto);
			case HAbstract(name) if (maps != null && ValueReader.nativeMapKind(name) != null):
				// a native map abstract: the pointer is the native map itself
				mapEntries(pointer, ValueReader.nativeMapKind(name));

			case HDynObj if (dynObjects != null):
				dynObjFields(pointer);

			case HObj(_), HStruct(_):
				objectFields(pointer, t);
			case HArray:
				varrayElements(pointer);
			case HEnum(proto) if (proto != null && enumLayout != null):
				enumParams(pointer, proto);

			case HVirtual(fields):
				virtualFields(pointer, fields);
			case HFun(_), HMethod(_):
				closureCapture(pointer);

			default:
				[];
		}
	}

	/**
		The address and static type of one named child (a field name, or an
		index as a string), for writing a value. It uses the same layout
		arithmetic as `of`, so a write lands exactly where the displayed value
		was read. Returns null when the child does not exist or has no address
		of its own (map entries, enum parameters, closure captures).
	**/
	public function targetOf(pointer:Pointer, t:HLType, childName:String):Null<AddressedValue> {
		return switch (t) {
			// arrays: a numeric name is an element; any other name is one of the
			// class's real fields, so `arr.length` resolves like an object field
			// (ArrayBase stores `length` as an I32 field)
			case HObj(proto) if (proto != null && ValueReader.arrayBytesElementType(proto.name) != null):
				asIndex(childName) >= 0
					? arrayBytesElementTarget(pointer, ValueReader.arrayBytesElementType(proto.name), childName)
					: objectFieldTarget(pointer, t, childName);
			case HObj(proto) if (proto != null && proto.name == "hl.types.ArrayObj"):
				asIndex(childName) >= 0
					? arrayObjElementTarget(pointer, childName)
					: objectFieldTarget(pointer, t, childName);
			case HObj(proto) if (proto != null && proto.name == ValueReader.ARRAY_DYN):
				arrayDynElementTarget(pointer, childName);

			case HDynObj if (dynObjects != null):
				var field = dynObjects.fieldByName(pointer, childName);
				field == null ? null : {address: field.address, type: field.type};

			case HObj(_), HStruct(_):
				objectFieldTarget(pointer, t, childName);
			case HArray:
				varrayElementTarget(pointer, childName);
			case HVirtual(fields):
				virtualFieldTarget(pointer, fields, childName);

			default:
				null;
		}
	}

	function objectFieldTarget(pointer:Pointer, t:HLType, name:String):Null<AddressedValue> {
		var proto = switch (t) {
			case HObj(p), HStruct(p): p;
			default: null;
		}
		if (proto == null) {
			return null;
		}
		for (field in objectLayout.fields(proto, t.match(HStruct(_)))) {
			if (field.name == name) {
				return {address: Int64.add(pointer, Int64.ofInt(field.offset)), type: field.type};
			}
		}
		return null;
	}

	function arrayBytesElementTarget(pointer:Pointer, elemType:HLType, name:String):Null<AddressedValue> {
		var index = asIndex(name);
		var length = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		if (index < 0 || index >= length) {
			return null;
		}
		var bytes = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (Int64.eq(bytes, Int64.ofInt(0))) {
			return null;
		}
		return {address: Int64.add(bytes, Int64.ofInt(index * align.typeSize(elemType))), type: elemType};
	}

	function arrayObjElementTarget(pointer:Pointer, name:String):Null<AddressedValue> {
		var index = asIndex(name);
		var length = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var native = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (index < 0 || index >= length || Int64.eq(native, Int64.ofInt(0))) {
			return null;
		}
		var elemType = varrayElementType(native);
		var base = Int64.add(native, Int64.ofInt(varrayHeaderSize()));
		return {address: Int64.add(base, Int64.ofInt(index * align.ptr)), type: elemType};
	}

	function arrayDynElementTarget(pointer:Pointer, name:String):Null<AddressedValue> {
		var inner = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr)));
		if (Int64.eq(inner, Int64.ofInt(0)) || runtimeTypes == null) {
			return null;
		}
		var refined = runtimeTypes.typeAt(mem.readPointer(inner));
		return switch (refined) {
			case HObj(p) if (p != null && p.name != ValueReader.ARRAY_DYN): targetOf(inner, refined, name);
			default: null;
		}
	}

	function varrayElementTarget(pointer:Pointer, name:String):Null<AddressedValue> {
		var index = asIndex(name);
		var size = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (index < 0 || index >= size) {
			return null;
		}
		var elemType = varrayElementType(pointer);
		var base = Int64.add(pointer, Int64.ofInt(varrayHeaderSize()));
		return {address: Int64.add(base, Int64.ofInt(index * align.typeSize(elemType))), type: elemType};
	}

	// vvirtual: the field's slot holds its address. A null slot means the field
	// lives on the wrapped dynamic object and has no address here.
	function virtualFieldTarget(pointer:Pointer, fields:Array<{name:String, t:HLType}>, name:String):Null<AddressedValue> {
		var objectBacked = !Int64.eq(mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr))), Int64.ofInt(0));
		for (i in 0...fields.length) {
			if (fields[i].name == name) {
				var slot = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * (3 + i))));
				if (Int64.eq(slot, Int64.ofInt(0))) {
					return null;
				}
				// in a virtual that wraps an object, a method slot points at machine
				// code; writing through it would overwrite the code
				return objectBacked && isFunctionField(fields[i].t) ? null : {address: slot, type: fields[i].t};
			}
		}
		return null;
	}

	static function isFunctionField(t:HLType):Bool {
		return switch (t) {
			case HFun(_), HMethod(_): true;
			default: false;
		}
	}

	static function asIndex(name:String):Int {
		var i = Std.parseInt(name);
		return i == null ? -1 : i;
	}

	// venum: one child per constructor parameter, at its EnumLayout offset
	function enumParams(pointer:Pointer, proto:EnumPrototype):Array<VariableInfo> {
		var index = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var variables:Array<VariableInfo> = [];
		for (param in enumLayout.params(proto, index)) {
			var decoded = reader.read(Int64.add(pointer, Int64.ofInt(param.offset)), param.type);
			variables.push({name: param.name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// vvirtual: header (t, value, next), then one slot per field holding the
	// field's address. A null slot means the field lives on the wrapped value,
	// usually a dynamic object, where it is looked up by name.
	function virtualFields(pointer:Pointer, fields:Array<{name:String, t:HLType}>):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		var wrapped = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var objectBacked = !Int64.eq(wrapped, Int64.ofInt(0));

		for (i in 0...fields.length) {
			var slot = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * (3 + i))));
			if (objectBacked && isFunctionField(fields[i].t) && !Int64.eq(slot, Int64.ofInt(0))) {
				// the slot is the method's code pointer, not an address to read
				var method = reader.readMethodPointer(slot, fields[i].t);
				variables.push({name: fields[i].name, value: method.value, type: method.type, reference: 0, kind: VariableKind.Field});
				continue;
			}
			if (Int64.eq(slot, Int64.ofInt(0))) {
				var fallback = wrappedField(wrapped, fields[i].name);
				if (fallback != null) {
					variables.push({name: fields[i].name, value: fallback.value, type: fallback.type, reference: fallback.reference, kind: VariableKind.Field});
				} else {
					variables.push({name: fields[i].name, value: "?", type: ValueReader.typeName(fields[i].t), reference: 0, kind: VariableKind.Field});
				}
				continue;
			}
			var decoded = reader.read(slot, fields[i].t);
			variables.push({name: fields[i].name, value: decoded.value, type: decoded.type, reference: decoded.reference, kind: VariableKind.Field});
		}
		return variables;
	}

	// A virtual field whose slot is null lives on the wrapped dynamic object.
	function wrappedField(wrapped:Pointer, name:String):Null<DecodedValue> {
		if (dynObjects == null || runtimeTypes == null || Int64.eq(wrapped, Int64.ofInt(0))) {
			return null;
		}
		var runtime = runtimeTypes.typeAt(mem.readPointer(wrapped));
		if (runtime == null || !runtime.match(HDynObj)) {
			return null;
		}
		var field = dynObjects.fieldByName(wrapped, name);
		return field != null ? reader.read(field.address, field.type) : null;
	}

	// vdynobj: one child per field
	function dynObjFields(pointer:Pointer):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (field in dynObjects.fields(pointer)) {
			var decoded = reader.read(field.address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference, kind: VariableKind.Field});
		}
		return variables;
	}

	// One child per entry of the native map at `native`, named by its key; the
	// values are read as Dynamic.
	function mapEntries(native:Pointer, kind:MapKeyKind):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (entry in maps.entries(native, kind, keyAddress -> reader.read(keyAddress, HDyn).value)) {
			var decoded = reader.read(entry.valueAddress, HDyn);
			variables.push({name: entry.key, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// EnumValueMap / BalancedTree entries, in key order
	function treeMapEntries(pointer:Pointer, proto:ObjPrototype):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (entry in treeMaps.entries(pointer, proto, keyAddress -> reader.read(keyAddress, HDyn).value)) {
			var decoded = reader.read(entry.valueAddress, HDyn);
			variables.push({name: entry.key, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// A bound vclosure (hasValue @ +ptr*2 == 1) has one "captured" child: its
	// bound object or capture environment, read as Dynamic.
	function closureCapture(pointer:Pointer):Array<VariableInfo> {
		var hasValue = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (hasValue != 1) {
			return [];
		}
		var decoded = reader.read(Int64.add(pointer, Int64.ofInt(align.ptr * 3)), HDyn);
		return [{name: "captured", value: decoded.value, type: decoded.type, reference: decoded.reference}];
	}

	function objectFields(pointer:Pointer, t:HLType):Array<VariableInfo> {
		var proto = switch (t) {
			case HObj(p), HStruct(p): p;
			default: null;
		}
		if (proto == null) {
			return [];
		}
		var variables:Array<VariableInfo> = [];
		for (field in objectLayout.fields(proto, t.match(HStruct(_)))) {
			// genhl adds one empty-named HVirtual field per implemented
			// interface, where hl_to_virtual caches the interface view of the
			// object. It is hidden from display only; the layout must keep it,
			// or every later field lands at the wrong offset.
			if (field.name == "" && field.type.match(HVirtual(_))) {
				continue;
			}
			var address = Int64.add(pointer, Int64.ofInt(field.offset));
			var decoded = reader.read(address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference, kind: VariableKind.Field});
		}
		return variables;
	}

	// hl.types.ArrayDyn (Array<Dynamic>) lists the elements of the ArrayBase it
	// wraps (@ +ptr), whose runtime type header gives its class (ArrayObj or
	// ArrayBytes_*). When that class cannot be resolved, it lists its fields.
	function arrayDynElements(pointer:Pointer, t:HLType):Array<VariableInfo> {
		var inner = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr)));
		if (Int64.eq(inner, Int64.ofInt(0))) {
			return [];
		}
		if (runtimeTypes != null) {
			var refined = runtimeTypes.typeAt(mem.readPointer(inner));
			switch (refined) {
				case HObj(p) if (p != null && p.name != ValueReader.ARRAY_DYN):
					return of(inner, refined);
				default:
			}
		}
		return objectFields(pointer, t);
	}

	// hl.types.ArrayBytes_<T>: length @ +ptr, bytes @ +ptr*2; the elements are
	// packed at the element type's size
	function arrayBytesElements(pointer:Pointer, elemType:HLType):Array<VariableInfo> {
		var length = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var bytes = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (length <= 0 || Int64.eq(bytes, Int64.ofInt(0))) {
			return [];
		}
		var stride = align.typeSize(elemType);
		return elements(length, i -> reader.read(Int64.add(bytes, Int64.ofInt(i * stride)), elemType));
	}

	// hl.types.ArrayObj: length @ +ptr, native varray @ +ptr*2. The varray's
	// pointer slots hold the elements; only the first `length` are in use.
	function arrayObjElements(pointer:Pointer):Array<VariableInfo> {
		var length = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var native = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (length <= 0 || Int64.eq(native, Int64.ofInt(0))) {
			return [];
		}
		var elemType = varrayElementType(native);
		var base = Int64.add(native, Int64.ofInt(varrayHeaderSize()));
		return elements(length, i -> reader.read(Int64.add(base, Int64.ofInt(i * align.ptr)), elemType));
	}

	// native varray: element type `at` @ +ptr, size @ +ptr*2, then the elements
	// packed at the element type's size
	function varrayElements(pointer:Pointer):Array<VariableInfo> {
		var size = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (size <= 0) {
			return [];
		}
		var elemType = varrayElementType(pointer);
		var stride = align.typeSize(elemType);
		var base = Int64.add(pointer, Int64.ofInt(varrayHeaderSize()));
		return elements(size, i -> reader.read(Int64.add(base, Int64.ofInt(i * stride)), elemType));
	}

	function varrayElementType(varray:Pointer):HLType {
		if (runtimeTypes != null) {
			var at = mem.readPointer(Int64.add(varray, Int64.ofInt(align.ptr)));
			var resolved = runtimeTypes.typeAt(at);
			if (resolved != null) {
				return resolved;
			}
		}
		return HDyn;
	}

	inline function varrayHeaderSize():Int {
		return align.ptr * 2 + 8; // t + at + size + __pad
	}

	function elements(length:Int, readAt:Int->DecodedValue):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		var shown = length > MAX_ELEMENTS ? MAX_ELEMENTS : length;
		for (i in 0...shown) {
			var decoded = readAt(i);
			variables.push({name: Std.string(i), value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		if (shown < length) {
			variables.push({name: "…", value: (length - shown) + " more elements not shown", type: "", reference: 0});
		}
		return variables;
	}
}

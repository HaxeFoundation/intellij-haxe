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
	Lists the children of an expandable value (a `variablesReference` target):
	object fields, array elements, and later enum params / virtual fields.
	Element listing is capped: a huge array gets a trailing "…" marker instead of
	flooding the client (no variable paging is advertised).
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
				// a map-native abstract: the reference pointer is the native map itself
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
		The address + static type of a single named child (a field name, or a
		numeric index as a string), for value modification. Reuses the SAME
		layout arithmetic as `of`, so a write lands exactly where the matching
		read came from. Returns null when the child isn't individually
		addressable (maps, enum params, closures) or doesn't exist.
	**/
	public function targetOf(pointer:Pointer, t:HLType, childName:String):Null<AddressedValue> {
		return switch (t) {
			// arrays: a numeric name is an element; anything else falls through to the
			// class's REAL fields (ArrayBase declares `length` as a physical I32), so
			// `arr.length` resolves like any object field
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

	// vvirtual: the field's indirect slot pointer (null slot = lives on the
	// wrapped dynobj, not directly addressable here)
	function virtualFieldTarget(pointer:Pointer, fields:Array<{name:String, t:HLType}>, name:String):Null<AddressedValue> {
		for (i in 0...fields.length) {
			if (fields[i].name == name) {
				var slot = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * (3 + i))));
				return Int64.eq(slot, Int64.ofInt(0)) ? null : {address: slot, type: fields[i].t};
			}
		}
		return null;
	}

	static function asIndex(name:String):Int {
		var i = Std.parseInt(name);
		return i == null ? -1 : i;
	}

	// venum: one child per constructor param at its EnumLayout offset
	function enumParams(pointer:Pointer, proto:EnumPrototype):Array<VariableInfo> {
		var index = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var variables:Array<VariableInfo> = [];
		for (param in enumLayout.params(proto, index)) {
			var decoded = reader.read(Int64.add(pointer, Int64.ofInt(param.offset)), param.type);
			variables.push({name: param.name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// vvirtual: header (t, value, next), then an indirect pointer per field; a
	// null field pointer means the field lives on the WRAPPED value (usually a
	// dynobj) — resolve it there by name instead of giving up
	function virtualFields(pointer:Pointer, fields:Array<{name:String, t:HLType}>):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		var wrapped = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr)));
		for (i in 0...fields.length) {
			var slot = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * (3 + i))));
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

	// runtime dynamic object: one child per lookup-table field
	function dynObjFields(pointer:Pointer):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (field in dynObjects.fields(pointer)) {
			var decoded = reader.read(field.address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference, kind: VariableKind.Field});
		}
		return variables;
	}

	// native map entries: name = key display, value read as a dynamic.
	// `native` is the native map pointer (already dereferenced from any wrapper).
	function mapEntries(native:Pointer, kind:MapKeyKind):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (entry in maps.entries(native, kind, keyAddress -> reader.read(keyAddress, HDyn).value)) {
			var decoded = reader.read(entry.valueAddress, HDyn);
			variables.push({name: entry.key, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// EnumValueMap / BalancedTree entries, walked in-order (sorted keys)
	function treeMapEntries(pointer:Pointer, proto:ObjPrototype):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (entry in treeMaps.entries(pointer, proto, keyAddress -> reader.read(keyAddress, HDyn).value)) {
			var decoded = reader.read(entry.valueAddress, HDyn);
			variables.push({name: entry.key, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// vclosure with a bound value (hasValue @ +ptr*2 == 1): one "captured"
	// child — the bound object or the capture environment — read as a dynamic
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
			var address = Int64.add(pointer, Int64.ofInt(field.offset));
			var decoded = reader.read(address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference, kind: VariableKind.Field});
		}
		return variables;
	}

	// hl.types.ArrayDyn (Array<Dynamic>): delegates to the wrapped ArrayBase
	// (@ +ptr), whose concrete class (ArrayObj / ArrayBytes_*) comes from its
	// runtime type header. Falls back to plain field expansion when unresolvable.
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

	// hl.types.ArrayBytes_<T>: length @ +ptr, bytes @ +ptr*2; elements packed at
	// the element type's own stride
	function arrayBytesElements(pointer:Pointer, elemType:HLType):Array<VariableInfo> {
		var length = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var bytes = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * 2)));
		if (length <= 0 || Int64.eq(bytes, Int64.ofInt(0))) {
			return [];
		}
		var stride = align.typeSize(elemType);
		return elements(length, i -> reader.read(Int64.add(bytes, Int64.ofInt(i * stride)), elemType));
	}

	// hl.types.ArrayObj: length @ +ptr, native varray @ +ptr*2; the varray's
	// pointer slots hold the elements (only `length` of them are live)
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

	// native varray: at @ +ptr (runtime element type), size @ +ptr*2, elements after
	// the header at the element type's stride
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

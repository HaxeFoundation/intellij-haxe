package debug.values;

import debug.Pointer;
import debug.layout.Align;
import debug.layout.EnumLayout;
import debug.layout.ObjectLayout;
import debug.target.MemoryReader;

import format.hl.Data.HLType;
import haxe.Int64;

/**
 * Lists the children of an expandable value (a `variablesReference` target):
 * object fields, array elements, and later enum params / virtual fields.
 * Element listing is capped: a huge array gets a trailing "…" marker instead of
 * flooding the client (no variable paging is advertised).
 */
class ValueChildren {
	static inline var MAX_ELEMENTS = 512;

	final mem:MemoryReader;
	final align:Align;
	final reader:ValueReader;
	final objectLayout:ObjectLayout;
	public var runtimeTypes:Null<RuntimeTypes> = null;
	public var enumLayout:Null<EnumLayout> = null;

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
			case HObj(_), HStruct(_):
				objectFields(pointer, t);
			case HArray:
				varrayElements(pointer);
			case HEnum(proto) if (proto != null && enumLayout != null):
				enumParams(pointer, proto);
			case HVirtual(fields):
				virtualFields(pointer, fields);
			default:
				[];
		}
	}

	// venum: one child per constructor param at its EnumLayout offset
	function enumParams(pointer:Pointer, proto:format.hl.Data.EnumPrototype):Array<VariableInfo> {
		var index = mem.readI32(Int64.add(pointer, Int64.ofInt(align.ptr)));
		var variables:Array<VariableInfo> = [];
		for (param in enumLayout.params(proto, index)) {
			var decoded = reader.read(Int64.add(pointer, Int64.ofInt(param.offset)), param.type);
			variables.push({name: param.name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// vvirtual: header (t, value, next), then an indirect pointer per field; a
	// null field pointer means the field lives on the wrapped dynobj (not read)
	function virtualFields(pointer:Pointer, fields:Array<{name:String, t:HLType}>):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (i in 0...fields.length) {
			var slot = mem.readPointer(Int64.add(pointer, Int64.ofInt(align.ptr * (3 + i))));
			if (Int64.eq(slot, Int64.ofInt(0))) {
				variables.push({name: fields[i].name, value: "?", type: ValueReader.typeName(fields[i].t), reference: 0});
				continue;
			}
			var decoded = reader.read(slot, fields[i].t);
			variables.push({name: fields[i].name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
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
		for (field in objectLayout.fields(proto)) {
			var address = Int64.add(pointer, Int64.ofInt(field.offset));
			var decoded = reader.read(address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
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

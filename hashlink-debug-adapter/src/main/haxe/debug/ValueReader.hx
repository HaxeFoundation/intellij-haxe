package debug;

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
		return switch (t) {
			case HObj(proto) if (proto != null && proto.name == "String"):
				leaf(readString(ptr), "String");
			default:
				expandableOrRaw(ptr, t);
		}
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
			default: false; // native arrays / others render raw until a later milestone
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
			case HObj(proto), HStruct(proto): proto != null ? displayName(proto.name) : "Object";
			case HVirtual(_): "Virtual";
			case HEnum(proto): proto != null ? proto.name : "Enum";
			case HNull(inner): typeName(inner);
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

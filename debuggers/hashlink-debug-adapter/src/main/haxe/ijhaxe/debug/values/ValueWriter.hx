package ijhaxe.debug.values;
import haxe.io.FPHelper;

import ijhaxe.debug.DebugError;
import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.target.MemoryReader;
import ijhaxe.debug.target.MemoryWriter;

import format.hl.Data.HLType;
import haxe.Int64;

/**
	Writes values into resolved target slots (`{address, type}`) while the
	debuggee is stopped. It never allocates: only the debuggee's own allocator
	can create heap values, which the eval-call machinery does before handing
	over the result. Supported writes:
	 - a literal into a matching primitive slot (Int, Float, Bool, Int64 and
	   the smaller integer types);
	 - `null` into any pointer slot;
	 - a call result: a pointer copy for reference types, a numeric
	   conversion for primitives;
	 - a primitive into an existing Null<T> box, or into a Dynamic box that
	   already holds that kind, updated in place.

	Any other combination throws a DebugError whose message is meant for the user.
**/
class ValueWriter {
	final mem:MemoryReader;
	final out:MemoryWriter;
	final align:Align;
	final runtimeTypes:RuntimeTypes;

	public function new(mem:MemoryReader, out:MemoryWriter, align:Align, runtimeTypes:RuntimeTypes) {
		this.mem = mem;
		this.out = out;
		this.align = align;
		this.runtimeTypes = runtimeTypes;
	}

	/**
		Writes a literal into the target.
	**/
	public function write(target:WriteTarget, literal:ValueLiteral):Void {
		// null into a Null<T> slot is a plain pointer write; any other value
		// updates the existing box in place
		if (literal.match(LNull) || !target.type.match(HNull(_))) {
			writeDirect(target, literal);
			return;
		}
		writeDirect(unwrapNullBox(target), literal);
	}

	function writeDirect(target:WriteTarget, literal:ValueLiteral):Void {
		switch (literal) {
			case LNull:
				writeNull(target);
			case LBool(value):
				writeBool(target, value);
			case LInt(value):
				writeInt(target, value);
			case LFloat(value):
				writeFloat(target, value);
		}
	}

	// A Null<T> slot points to a box: type @ +0, value @ Align.dynPayload.
	// Returns the box's value as the target; a null box cannot be updated.
	function unwrapNullBox(target:WriteTarget):WriteTarget {
		var inner = switch (target.type) {
			case HNull(t): t;
			default: target.type;
		}
		var box = mem.readPointer(target.address);
		if (Int64.compare(box, Int64.ofInt(0)) == 0) {
			throw new DebugError('Cannot assign to "' + target.name
				+ '" because it is currently null (allocating a new boxed value is not supported)');
		}
		return {name: target.name, address: box.offset(align.dynPayload), type: inner};
	}

	/**
		Writes a call's raw result (RAX, or the double's bits for a float return)
		into the target, checked against the result type `sourceType`. A pointer
		result is written as is, since the callee returned a live heap object.
		A primitive is converted to the target type.
	**/
	public function assignRaw(target:WriteTarget, raw:Int64, sourceType:HLType):Void {
		if (target.type.match(HNull(_))) {
			if (isPointer(sourceType) && Int64.compare(raw, Int64.ofInt(0)) == 0) {
				out.writePointer(target.address, Int64.ofInt(0));
				return;
			}
			assignRaw(unwrapNullBox(target), raw, sourceType);
			return;
		}
		if (isPointer(target.type)) {
			if (!isPointer(sourceType)) {
				throw new DebugError('Cannot assign ' + ValueReader.typeName(sourceType)
					+ ' to the reference "' + target.name + '"');
			}
			out.writePointer(target.address, raw);
			return;
		}
		if (isFloat(target.type)) {
			writeFloat(target, rawAsFloat(raw, sourceType));
			return;
		}
		if (target.type.match(HBool)) {
			writeBool(target, Int64.compare(rawAsInt(raw, sourceType), Int64.ofInt(0)) != 0);
			return;
		}
		writeInt(target, rawAsInt(raw, sourceType));
	}

	// A call result's raw RAX bits, interpreted by the return type.
	static function rawAsInt(raw:Int64, sourceType:HLType):Int64 {
		return switch (sourceType) {
			case HUi8: Int64.ofInt(raw.low & 0xFF);
			case HUi16: Int64.ofInt(raw.low & 0xFFFF);
			case HI32, HBool: Int64.ofInt(raw.low);
			case HI64: raw;
			case HF64: Int64.fromFloat(FPHelper.i64ToDouble(raw.low, raw.high));
			case HF32: Int64.fromFloat(FPHelper.i32ToFloat(raw.low));
			default: throw new DebugError("Cannot assign a " + ValueReader.typeName(sourceType) + " result to a number");
		}
	}

	static function rawAsFloat(raw:Int64, sourceType:HLType):Float {
		return switch (sourceType) {
			case HF64: FPHelper.i64ToDouble(raw.low, raw.high);
			case HF32: FPHelper.i32ToFloat(raw.low);
			default: int64ToFloat(rawAsInt(raw, sourceType));
		}
	}

	function writeNull(target:WriteTarget):Void {
		if (!isPointer(target.type)) {
			throw new DebugError('Cannot assign null to the ' + ValueReader.typeName(target.type)
				+ ' "' + target.name + '"');
		}
		out.writePointer(target.address, Int64.ofInt(0));
	}

	function writeBool(target:WriteTarget, value:Bool):Void {
		switch (target.type) {
			case HBool:
				out.writeU8(target.address, value ? 1 : 0);
			case HDyn:
				mutateBox(target, HBool, box -> out.writeU8(box, value ? 1 : 0));
			default:
				throw new DebugError('Cannot assign a boolean to the ' + ValueReader.typeName(target.type)
					+ ' "' + target.name + '"');
		}
	}

	function writeInt(target:WriteTarget, value:Int64):Void {
		switch (target.type) {
			case HUi8: out.writeU8(target.address, value.low);
			case HUi16: out.writeU16(target.address, value.low);
			case HI32: out.writeI32(target.address, value.low);
			case HI64: out.writeI64(target.address, value);
			case HF32: out.writeF32(target.address, int64ToFloat(value));
			case HF64: out.writeF64(target.address, int64ToFloat(value));
			case HDyn: mutateBox(target, HI32, box -> out.writeI32(box, value.low));
			default:
				throw new DebugError('Cannot assign an integer to the ' + ValueReader.typeName(target.type)
					+ ' "' + target.name + '"');
		}
	}

	function writeFloat(target:WriteTarget, value:Float):Void {
		switch (target.type) {
			case HF32: out.writeF32(target.address, value);
			case HF64: out.writeF64(target.address, value);
			case HDyn: mutateBox(target, HF64, box -> out.writeF64(box, value));
			default:
				throw new DebugError('Cannot assign a float to the ' + ValueReader.typeName(target.type)
					+ ' "' + target.name + '"');
		}
	}

	// A Dynamic slot points to a vdynamic: runtime type @ +0, payload @
	// Align.dynPayload. The box can only be updated in place when it already
	// holds the same primitive kind; anything else needs a new allocation.
	function mutateBox(target:WriteTarget, wantKind:HLType, writePayload:Pointer->Void):Void {
		var box = mem.readPointer(target.address);
		if (Int64.compare(box, Int64.ofInt(0)) == 0) {
			throw new DebugError('Cannot assign into null Dynamic "' + target.name
				+ '" (creating a boxed value needs allocation)');
		}
		var boxed = runtimeTypes == null ? null : runtimeTypes.typeAt(mem.readPointer(box));
		if (boxed == null || !sameKind(boxed, wantKind)) {
			throw new DebugError('Cannot change the type of Dynamic "' + target.name
				+ '" in place (currently ' + (boxed == null ? "unknown" : ValueReader.typeName(boxed))
				+ "; allocating a new boxed value is not supported)");
		}
		writePayload(box.offset(align.dynPayload));
	}

	static function sameKind(a:HLType, b:HLType):Bool {
		return Type.enumIndex(a) == Type.enumIndex(b);
	}

	static function isFloat(t:HLType):Bool {
		return t.match(HF32) || t.match(HF64);
	}

	static function isPointer(t:HLType):Bool {
		return switch (t) {
			case HVoid, HUi8, HUi16, HI32, HI64, HF32, HF64, HBool: false;
			default: true;
		}
	}

	/**
		An Int64 as a Float: `high * 2^32 + low`, with `low` read as unsigned.

		4294967296.0 is 2^32. It is both the weight of the high word and the
		offset that turns a negative (two's-complement) low word into its
		unsigned value.

		A Float has a 53-bit mantissa, so magnitudes above 2^53 round to the
		nearest representable value. The multiplication is exact, because
		scaling by a power of two only changes the exponent; the addition is
		the only rounding step.
	**/
	static function int64ToFloat(v:Int64):Float {
		var low = v.low;
		var lowUnsigned = low < 0 ? low + 4294967296.0 : low;
		return v.high * 4294967296.0 + lowUnsigned;
	}
}

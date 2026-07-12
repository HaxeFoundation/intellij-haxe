package debug.values;

import debug.DebugError;
import debug.Pointer;
import debug.layout.Align;
import debug.target.MemoryReader;
import debug.target.MemoryWriter;

import format.hl.Data.HLType;
import haxe.Int64;

/**
 * Writes a value into a resolved target slot `{address, type}` from a parsed
 * ValueLiteral. The envelope is deliberately allocation-free — creating new
 * heap values (strings, objects) needs the debuggee's allocator (the eval-call
 * machinery, a later milestone). Supported:
 *  - a literal into a matching primitive slot (Int/Float/Bool/Int64/sub-int);
 *  - `null` into any pointer slot;
 *  - a variable path whose EXISTING value is copied — a raw pointer copy for
 *    reference types, a numeric coercion for primitives;
 *  - an in-place primitive update of a Dynamic that already boxes that kind.
 *
 * Only runs while the debuggee is stopped. Any unsupported combination throws
 * a DebugError with a message aimed at the user.
 */
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

	/** A resolved write source: the value read from a variable path. */
	public function write(target:WriteTarget, literal:ValueLiteral):Void {
		// setting a nullable slot to null is a plain pointer write; any other
		// value updates the box it points at (allocating a fresh box is not
		// possible from the adapter)
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
			case LPath(_):
				throw new DebugError("internal: path literals are resolved by the caller");
		}
	}

	// A Null<T> slot holds a pointer to a box [type @ 0][value @ +ptr]. Returns
	// a target on the box's payload; a null box can't be updated in place.
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
		return {name: target.name, address: offset(box, align.ptr), type: inner};
	}

	/** Copies an already-resolved source slot into the target (variable = variable). */
	public function copy(target:WriteTarget, source:WriteTarget):Void {
		if (target.type.match(HNull(_))) {
			// null source into a nullable slot clears it; otherwise update the box
			if (isPointer(source.type) && Int64.compare(mem.readPointer(source.address), Int64.ofInt(0)) == 0) {
				out.writePointer(target.address, Int64.ofInt(0));
				return;
			}
			copy(unwrapNullBox(target), source);
			return;
		}
		if (source.type.match(HNull(_))) {
			copy(target, unwrapNullBox(source));
			return;
		}
		if (isPointer(target.type)) {
			if (!isPointer(source.type)) {
				throw new DebugError('Cannot assign ' + ValueReader.typeName(source.type)
					+ ' to the reference "' + target.name + '"');
			}
			out.writePointer(target.address, mem.readPointer(source.address));
			return;
		}
		// primitive target: read the source as a number and coerce
		if (isFloat(target.type)) {
			writeFloat(target, readNumericAsFloat(source));
			return;
		}
		if (target.type.match(HBool)) {
			writeBool(target, Int64.compare(readNumericAsInt(source), Int64.ofInt(0)) != 0);
			return;
		}
		writeInt(target, readNumericAsInt(source));
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
			case HUi8: out.writeU8(target.address, Int64.getLow(value));
			case HUi16: out.writeU16(target.address, Int64.getLow(value));
			case HI32: out.writeI32(target.address, Int64.getLow(value));
			case HI64: out.writeI64(target.address, value);
			case HF32: out.writeF32(target.address, int64ToFloat(value));
			case HF64: out.writeF64(target.address, int64ToFloat(value));
			case HDyn: mutateBox(target, HI32, box -> out.writeI32(box, Int64.getLow(value)));
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

	// A Dynamic slot holds a pointer to a vdynamic (runtime type @ +0, payload
	// @ +ptr). We can update a box IN PLACE only when it already holds the same
	// primitive kind — anything else would need a fresh allocation.
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
		writePayload(offset(box, align.ptr));
	}

	function readNumericAsInt(source:WriteTarget):Int64 {
		return switch (source.type) {
			case HUi8: Int64.ofInt(mem.readU8(source.address));
			case HUi16: Int64.ofInt(mem.readU16(source.address));
			case HI32: Int64.ofInt(mem.readI32(source.address));
			case HI64: mem.readI64(source.address);
			case HBool: Int64.ofInt(mem.readU8(source.address) != 0 ? 1 : 0);
			default:
				throw new DebugError("Can only copy a numeric value into a primitive slot (source is "
					+ ValueReader.typeName(source.type) + ")");
		}
	}

	function readNumericAsFloat(source:WriteTarget):Float {
		return switch (source.type) {
			case HF32: mem.readF32(source.address);
			case HF64: mem.readF64(source.address);
			default: int64ToFloat(readNumericAsInt(source));
		}
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

	static function int64ToFloat(v:Int64):Float {
		var low = Int64.getLow(v);
		var lowUnsigned = low < 0 ? low + 4294967296.0 : low;
		return Int64.getHigh(v) * 4294967296.0 + lowUnsigned;
	}

	static inline function offset(p:Pointer, n:Int):Pointer {
		return Int64.add(p, Int64.ofInt(n));
	}
}

package ijhaxe.debug.values;
import format.hl.Tools;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.target.MemoryReader;
import format.hl.Data.HLType;
import haxe.Int64;

/**
	Reads dynamic objects (vdynobj): what a `Dynamic`-typed structure, a
	Reflect-built object or parsed JSON becomes at run time.

	Layout (a port of hld readFieldAddress/dfields):

	| field      | offset   | purpose                                    |
	|------------|----------|--------------------------------------------|
	| `lookup`   | `+ptr`   | sorted-by-hash table, one entry per field  |
	| `raw_data` | `+2*ptr` | storage for non-pointer field values       |
	| `values`   | `+3*ptr` | storage for pointer field values           |
	| `nfields`  | `+4*ptr` |                                            |

	Lookup entry `i` is at `lookup + i * Align.fieldLookupStride`: `hl_type*`
	@ +0, `hashed_name` i32 @ +ptr, `packed` i32 @ +ptr+4. The low 17 bits of
	`packed` are the slot offset; `packed >>> 17` is the field's display order.

	Field names are stored as hl_hash values and turned back into names
	through the module's string table, which contains every field name that
	appears in the program's source.
**/
class DynObjReader {
	static inline var OFFSET_MASK = (1 << 17) - 1;
	static inline var MAX_FIELDS = 4096; // a larger count means corrupt memory

	final mem:MemoryReader;
	final align:Align;
	final runtimeTypes:RuntimeTypes;
	final resolveHash:Int->Null<String>;

	public function new(mem:MemoryReader, align:Align, runtimeTypes:RuntimeTypes, resolveHash:Int->Null<String>) {
		this.mem = mem;
		this.align = align;
		this.runtimeTypes = runtimeTypes;
		this.resolveHash = resolveHash;
	}

	/**
		Number of fields of the dynamic object at `ptr`, or 0 when the count is implausible.
	**/
	public function fieldCount(ptr:Pointer):Int {
		var n = mem.readI32(ptr.offset(align.ptr * 4));
		return (n >= 0 && n <= MAX_FIELDS) ? n : 0;
	}

	/**
		All fields, in display order when the lookup entries record one.
	**/
	public function fields(ptr:Pointer):Array<DynObjField> {
		var count = fieldCount(ptr);
		if (count == 0) {
			return [];
		}
		var lookup = mem.readPointer(ptr.offset(align.ptr));
		var rawData = mem.readPointer(ptr.offset(align.ptr * 2));
		var values = mem.readPointer(ptr.offset(align.ptr * 3));

		var ordered:Array<Null<DynObjField>> = [];
		var unordered:Array<DynObjField> = [];
		var hasIndex = false;
		for (i in 0...count) {
			var entry = lookup.offset(i * align.fieldLookupStride);
			var fieldType = runtimeTypes.typeAt(mem.readPointer(entry));
			if (fieldType == null) {
				fieldType = HDyn;
			}
			var hash = mem.readI32(entry.offset(align.ptr));
			var packed = mem.readI32(entry.offset(align.ptr + 4));
			var slot = packed & OFFSET_MASK;
			var index = packed >>> 17;
			if (index > 0) {
				hasIndex = true;
			}
			var name = resolveHash(hash);
			if (name == null) {
				name = "field#" + hash;
			}
			var address = isPointerType(fieldType)
				? values.offset(slot * align.ptr)
				: rawData.offset(slot);
			var field:DynObjField = {name: name, address: address, type: fieldType};
			if (hasIndex) {
				ordered[index] = field;
			} else {
				unordered.push(field);
			}
		}
		if (!hasIndex) {
			return unordered;
		}
		return [for (f in ordered) if (f != null) f];
	}

	/**
		The named field, found by a binary search for its hash in the lookup table, or null.
	**/
	public function fieldByName(ptr:Pointer, name:String):Null<DynObjField> {
		var count = fieldCount(ptr);
		if (count == 0) {
			return null;
		}
		var lookup = mem.readPointer(ptr.offset(align.ptr));
		var rawData = mem.readPointer(ptr.offset(align.ptr * 2));
		var values = mem.readPointer(ptr.offset(align.ptr * 3));
		var hash = Tools.hash(name);

		var min = 0;
		var max = count;

		while (min < max) {
			var mid = (min + max) >> 1;
			var entry = lookup.offset(mid * align.fieldLookupStride);
			var h = mem.readI32(entry.offset(align.ptr));
			if (h < hash) {
				min = mid + 1;
			} else if (h > hash) {
				max = mid;
			} else {
				var fieldType = runtimeTypes.typeAt(mem.readPointer(entry));
				if (fieldType == null) {
					fieldType = HDyn;
				}
				var slot = mem.readI32(entry.offset(align.ptr + 4)) & OFFSET_MASK;
				var address = isPointerType(fieldType)
					? values.offset(slot * align.ptr)
					: rawData.offset(slot);
				return {name: name, address: address, type: fieldType};
			}
		}
		return null;
	}

	static function isPointerType(t:HLType):Bool {
		return switch (t) {
			case HVoid, HUi8, HUi16, HI32, HI64, HF32, HF64, HBool: false;
			default: true;
		}
	}
}

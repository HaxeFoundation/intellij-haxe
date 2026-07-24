package ijhaxe.debug.layout;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

/**
	Computes the byte offset of each field within a HashLink object instance,
	replicating the runtime layout (a port of hld `getObjectProto`, matching the
	VM's `hl_runtime_obj`): an object begins with a `hl_type*` header (one
	pointer) — a struct does NOT — superclass fields come first (reclaiming the
	parent's trailing padding), and each field is aligned to its own size.

	A `@:packed` field (HPacked wrapping an HStruct) is inlined: it is aligned
	on the sub-struct's largest field and occupies the sub-struct's full padded
	size. The total size is padded to a multiple of the largest field, which is
	what makes nested packed layouts compose.

	Pure; unit-tested with synthetic prototypes.
**/
class ObjectLayout {
	final align:Align;
	final cache:Map<String, ProtoLayout> = new Map();

	public function new(align:Align) {
		this.align = align;
	}

	/**
		All fields of `proto` (superclass fields first) with their instance
		offsets. `isStruct` skips the hl_type* header (HStruct values and the
		inline layout of packed fields).
	**/
	public function fields(proto:ObjPrototype, isStruct:Bool = false):Array<FieldLayout> {
		return layout(proto, isStruct).fields;
	}

	function layout(proto:ObjPrototype, isStruct:Bool):ProtoLayout {
		var key = (isStruct ? "~" : "") + proto.name;
		var cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		var parent = proto.tsuper == null ? null : switch (proto.tsuper) {
			case HObj(p), HStruct(p): layout(p, isStruct);
			default: null;
		};
		var fields = parent == null ? [] : parent.fields.copy();
		// the parent's trailing padding is reclaimed before appending our fields
		var size = parent == null ? (isStruct ? 0 : align.ptr) : parent.size - parent.padSize;
		var largestField = parent == null ? size : parent.largestField;
		for (field in proto.fields) {
			switch (field.t) {
				case HPacked({v: HStruct(sub)}):
					// an inlined @:packed sub-struct: aligned on and sized by the sub-struct
					var packed = layout(sub, true);
					size = alignUp(size, packed.largestField);
					if (packed.largestField > largestField) {
						largestField = packed.largestField;
					}
					fields.push({name: field.name, offset: size, type: field.t});
					size += packed.size;
				default:
					size += align.padStruct(size, field.t);
					fields.push({name: field.name, offset: size, type: field.t});
					var fieldSize = align.typeSize(field.t);
					if (fieldSize > largestField) {
						largestField = fieldSize;
					}
					size += fieldSize;
			}
		}
		// pad the total to a multiple of the largest field so nested layouts compose
		var padSize = alignUp(size, largestField) - size;
		size += padSize;
		var result = {fields: fields, size: size, padSize: padSize, largestField: largestField};
		cache.set(key, result);
		return result;
	}

	// Rounds `value` up to the next multiple of `to` (a no-op when `to` <= 0).
	static inline function alignUp(value:Int, to:Int):Int {
		var rem = to <= 0 ? 0 : value % to;
		return rem == 0 ? value : value + (to - rem);
	}
}

private typedef ProtoLayout = {
	fields:Array<FieldLayout>,
	size:Int,
	padSize:Int,
	largestField:Int,
}

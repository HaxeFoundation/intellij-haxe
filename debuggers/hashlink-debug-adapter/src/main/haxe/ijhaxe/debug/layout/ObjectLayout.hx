package ijhaxe.debug.layout;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

/**
	Computes the byte offset of each field within a HashLink object, the way
	the VM lays it out (a port of hld `getObjectProto`, matching the VM's
	`hl_runtime_obj`). An object starts with an `hl_type*` header; a struct has
	none. Superclass fields come first, and the subclass's first field may use
	the superclass's trailing padding. Each field is aligned by the C struct
	rules (Align.padStruct).

	A `@:packed` field (an HPacked wrapping an HStruct) stores the sub-struct
	inline: aligned on the sub-struct's largest field and occupying its full
	padded size. Every layout's total size is padded to a multiple of its
	largest field, so packed structs nest correctly.
**/
class ObjectLayout {
	final align:Align;
	final cache:Map<String, ProtoLayout> = new Map();

	public function new(align:Align) {
		this.align = align;
	}

	/**
		All fields of `proto`, superclass fields first, with their offsets.
		`isStruct` omits the hl_type* header, for HStruct values and for the
		inline contents of packed fields.
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
		// this type's fields may start inside the parent's trailing padding
		var size = parent == null ? (isStruct ? 0 : align.ptr) : parent.size - parent.padSize;
		var largestField = parent == null ? size : parent.largestField;

		for (field in proto.fields) {
			switch (field.t) {
				case HPacked({v: HStruct(sub)}):
					// a @:packed sub-struct, stored inline
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
		// a multiple of the largest field, so this layout can be nested as a packed field
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

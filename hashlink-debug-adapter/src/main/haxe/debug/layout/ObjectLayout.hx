package debug.layout;

import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;

/**
 * Computes the byte offset of each field within a HashLink object instance,
 * replicating the runtime layout (a port of hld `getObjectProto`, matching the
 * VM's `hl_runtime_obj`): an object begins with a `hl_type*` header (one pointer),
 * superclass fields come first, and each field is aligned to its own size.
 *
 * Pure; unit-tested with synthetic prototypes. Nested inline structs
 * (HStruct/HPacked as fields) are treated as pointer-sized for now.
 */
class ObjectLayout {
	final align:Align;
	final cache:Map<String, Array<FieldLayout>> = new Map();

	public function new(align:Align) {
		this.align = align;
	}

	/** All fields of `proto` (superclass fields first) with their instance offsets. */
	public function fields(proto:ObjPrototype):Array<FieldLayout> {
		var key = proto.name;
		var cached = cache.get(key);
		if (cached != null) {
			return cached;
		}
		var result = layout(proto).fields;
		cache.set(key, result);
		return result;
	}

	function layout(proto:ObjPrototype):{fields:Array<FieldLayout>, size:Int} {
		var fields:Array<FieldLayout>;
		var size:Int;
		var superProto = proto.tsuper == null ? null : switch (proto.tsuper) {
			case HObj(parent), HStruct(parent): parent;
			default: null;
		};
		if (superProto != null) {
			var base = layout(superProto);
			fields = base.fields.copy();
			size = base.size;
		} else {
			fields = [];
			size = align.ptr; // the hl_type* header occupies the first pointer
		}
		for (field in proto.fields) {
			size += align.padSize(size, field.t);
			fields.push({name: field.name, offset: size, type: field.t});
			size += align.typeSize(field.t);
		}
		return {fields: fields, size: size};
	}
}

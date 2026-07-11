package debug;

import format.hl.Data.EnumPrototype;

/**
 * Computes the byte offsets of an enum value's constructor parameters,
 * replicating the VM's venum layout (a port of hld `getEnumProto` / the VM's
 * hl_init_enum): header = hl_type* + i32 constructor index, then each param
 * aligned with the C struct rules (Align.padStruct) — which can pack a param
 * into the header's tail padding (an i32 param lands at +12 on 64-bit).
 */
class EnumLayout {
	final align:Align;

	public function new(align:Align) {
		this.align = align;
	}

	/** Offsets/types of constructor `index`'s params, named by position. */
	public function params(proto:EnumPrototype, index:Int):Array<FieldLayout> {
		if (index < 0 || index >= proto.constructs.length) {
			return [];
		}
		var construct = proto.constructs[index];
		var size = align.ptr;
		size += align.padStruct(size, HI32);
		size += 4; // the constructor index
		var result:Array<FieldLayout> = [];
		for (i in 0...construct.params.length) {
			var t = construct.params[i];
			size += align.padStruct(size, t);
			result.push({name: Std.string(i), offset: size, type: t});
			size += align.typeSize(t);
		}
		return result;
	}
}

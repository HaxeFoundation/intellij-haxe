package ijhaxe.debug.layout;

import format.hl.Data.EnumPrototype;

/**
	Computes the byte offsets of an enum value's constructor parameters, the
	way the VM lays out a venum (a port of hld `getEnumProto`, matching the
	VM's hl_init_enum). The header is an hl_type* plus the i32 constructor
	index. Each parameter follows, aligned by the C struct rules
	(Align.padStruct), so a parameter can occupy the header's tail padding:
	an i32 parameter lands at +12 on 64-bit.
**/
class EnumLayout {
	final align:Align;

	public function new(align:Align) {
		this.align = align;
	}

	/**
		Offset and type of each parameter of constructor `index`, named by position.
	**/
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

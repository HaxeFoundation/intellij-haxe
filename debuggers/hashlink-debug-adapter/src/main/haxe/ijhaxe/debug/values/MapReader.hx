package ijhaxe.debug.values;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.target.MemoryReader;
import haxe.Int64;

/**
	Reads native HashLink maps, which haxe.ds.StringMap, IntMap and ObjectMap
	keep in their first field. A port of hld makeMap for the layout of HL
	runtime 1.13 and later. Older runtimes get no entries, and callers show
	the raw map instead.

	Layout:

	| field     | offset   | purpose                                       |
	|-----------|----------|-----------------------------------------------|
	| `cells`   | `+0`     | hash buckets: first entry index per bucket    |
	| `nexts`   | `+ptr`   | per-entry chain links                         |
	| `entries` | `+2*ptr` | key storage (Int keys)                        |
	| `values`  | `+3*ptr` | value storage (+ keys for String/Object maps) |

	A freelist (ptr + 8 bytes) and the i32s ncells, nentries and maxEntries
	follow. Small maps (maxEntries < 128) store cells and nexts as bytes, with
	255 ending a chain. Larger maps use i32 arrays, where a negative value
	ends a chain.
**/
class MapReader {
	static inline var MAX_ENTRIES = 512; // same cap as array listing
	static inline var MAX_KEY_CHARS = 256;
	static inline var SMALL_MAP_LIMIT = 128;

	final mem:MemoryReader;
	final align:Align;
	final supported:Bool; // HL runtime >= 1.13

	public function new(mem:MemoryReader, align:Align, supported:Bool) {
		this.mem = mem;
		this.align = align;
		this.supported = supported;
	}

	/**
		The number of entries, or -1 when the layout is unsupported or the count is implausible.
	**/
	public function entryCount(native:Pointer):Int {
		if (!supported || native.isNull()) {
			return -1;
		}
		var counts = countsOffset();
		var nentries = mem.readI32(native.offset(counts + 4));
		return (nentries >= 0 && nentries <= 1 << 24) ? nentries : -1;
	}

	/**
		The entries, at most MAX_ENTRIES of them. `objectKeyPreview` renders the
		Dynamic key at an address, for ObjectMap keys.
	**/
	public function entries(native:Pointer, kind:MapKeyKind, objectKeyPreview:Pointer->String):Array<MapEntrySlot> {
		var total = entryCount(native);
		if (total <= 0) {
			return [];
		}
		var cells = mem.readPointer(native);
		var nexts = mem.readPointer(native.offset(align.ptr));
		var entries = mem.readPointer(native.offset(align.ptr * 2));
		var values = mem.readPointer(native.offset(align.ptr * 3));
		var counts = countsOffset();
		var ncells = mem.readI32(native.offset(counts));
		var maxEntries = mem.readI32(native.offset(counts + 8));
		if (ncells <= 0 || ncells > 1 << 24) {
			return [];
		}
		var small = maxEntries < SMALL_MAP_LIMIT;

		// where each key kind stores its keys and values
		var keyInValue;
		var valuePos;
		var keyStride;
		var valueStride;

		switch (kind) {
			case StringKey:
				keyInValue = true;
				valuePos = align.ptr;
				keyStride = 4;
				valueStride = align.ptr * 2;
			case IntKey:
				keyInValue = false;
				valuePos = 0;
				keyStride = 4;
				valueStride = align.ptr;
			case Int64Key:
				keyInValue = false;
				valuePos = 0;
				keyStride = 8;
				valueStride = align.ptr;
			case ObjectKey:
				keyInValue = true;
				valuePos = align.ptr;
				keyStride = 0;
				valueStride = align.ptr * 2;
		}

		var result:Array<MapEntrySlot> = [];
		var cell = 0;
		while (cell < ncells && result.length < total && result.length < MAX_ENTRIES) {
			var c = small ? mem.readU8(cells.offset(cell)) : mem.readI32(cells.offset(cell << 2));
			cell++;
			while (result.length < total && result.length < MAX_ENTRIES) {
				if (small ? c == 255 : c < 0) {
					break;
				}
				var valueAddress = values.offset(c * valueStride + valuePos);
				var keyAddress = keyInValue ? values.offset(c * valueStride) : entries.offset(c * keyStride);
				var key = switch (kind) {
					case StringKey: '"${readUcs2(mem.readPointer(keyAddress))}"';
					case IntKey: Std.string(mem.readI32(keyAddress));
					case Int64Key: haxe.Int64.toStr(mem.readI64(keyAddress));
					case ObjectKey: objectKeyPreview(keyAddress);
				}
				result.push({key: key, valueAddress: valueAddress});
				c = small ? mem.readU8(nexts.offset(c)) : mem.readI32(nexts.offset(c << 2));
			}
		}
		return result;
	}

	// the counts follow the four table pointers and the freelist (ptr + 4 + 4 bytes)
	inline function countsOffset():Int {
		return align.ptr * 4 + (align.ptr + 4 + 4);
	}

	// a NUL-terminated UCS-2 key: raw characters, not a String object
	function readUcs2(bytes:Pointer):String {
		if (bytes.isNull()) {
			return "";
		}
		var raw = mem.read(bytes, MAX_KEY_CHARS * 2);
		var buf = new StringBuf();
		for (i in 0...MAX_KEY_CHARS) {
			var c = raw.getUInt16(i * 2);
			if (c == 0) {
				break;
			}
			buf.addChar(c);
		}
		return buf.toString();
	}
}

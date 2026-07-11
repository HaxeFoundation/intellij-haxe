package debug.values;

import debug.Pointer;
import debug.layout.Align;
import debug.target.MemoryReader;
import haxe.Int64;

/**
 * Reads native HashLink maps (what haxe.ds.StringMap/IntMap/ObjectMap wrap in
 * their first field). Port of hld makeMap for the HL RUNTIME >= 1.13 layout —
 * older runtimes get no entry listing (callers fall back to a raw display).
 *
 * Native layout (64-bit):
 *   cells   @ +0       hash buckets: first entry index per bucket
 *   nexts   @ +ptr     per-entry chain links
 *   entries @ +2*ptr   key storage (Int keys)
 *   values  @ +3*ptr   value storage (+ keys for String/Object maps)
 *   freelist (ptr+8 bytes), then ncells/nentries/maxEntries i32s
 * Small maps (maxEntries < 128) use BYTE cells/nexts with 255 as the chain
 * terminator; larger maps use i32 arrays with negative terminators.
 */
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

	/** Live entry count, or -1 when the layout is unsupported/implausible. */
	public function entryCount(native:Pointer):Int {
		if (!supported || isNull(native)) {
			return -1;
		}
		var counts = countsOffset();
		var nentries = mem.readI32(offset(native, counts + 4));
		return (nentries >= 0 && nentries <= 1 << 24) ? nentries : -1;
	}

	/**
	 * The live entries. `dynPreview` renders Object keys (read as HDyn) for
	 * display. Capped at 512 entries.
	 */
	public function entries(native:Pointer, kind:MapKeyKind, dynPreview:Pointer->String):Array<MapEntrySlot> {
		var total = entryCount(native);
		if (total <= 0) {
			return [];
		}
		var cells = mem.readPointer(native);
		var nexts = mem.readPointer(offset(native, align.ptr));
		var entries = mem.readPointer(offset(native, align.ptr * 2));
		var values = mem.readPointer(offset(native, align.ptr * 3));
		var counts = countsOffset();
		var ncells = mem.readI32(offset(native, counts));
		var maxEntries = mem.readI32(offset(native, counts + 8));
		if (ncells <= 0 || ncells > 1 << 24) {
			return [];
		}
		var small = maxEntries < SMALL_MAP_LIMIT;

		// per-kind strides (HL >= 1.13)
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
			var c = small ? mem.readU8(offset(cells, cell)) : mem.readI32(offset(cells, cell << 2));
			cell++;
			while (result.length < total && result.length < MAX_ENTRIES) {
				if (small ? c == 255 : c < 0) {
					break;
				}
				var valueAddress = offset(values, c * valueStride + valuePos);
				var keyAddress = keyInValue ? offset(values, c * valueStride) : offset(entries, c * keyStride);
				var key = switch (kind) {
					case StringKey: "\"" + readUcs2(mem.readPointer(keyAddress)) + "\"";
					case IntKey: Std.string(mem.readI32(keyAddress));
					case Int64Key: haxe.Int64.toStr(mem.readI64(keyAddress));
					case ObjectKey: dynPreview(keyAddress);
				}
				result.push({key: key, valueAddress: valueAddress});
				c = small ? mem.readU8(offset(nexts, c)) : mem.readI32(offset(nexts, c << 2));
			}
		}
		return result;
	}

	// freelist (ptr + 4 + 4 bytes) sits after the four table pointers
	inline function countsOffset():Int {
		return align.ptr * 4 + (align.ptr + 4 + 4);
	}

	// null-terminated UCS-2 key bytes (not a String object)
	function readUcs2(bytes:Pointer):String {
		if (isNull(bytes)) {
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

	static inline function offset(p:Pointer, n:Int):Pointer {
		return Int64.add(p, Int64.ofInt(n));
	}

	static inline function isNull(p:Pointer):Bool {
		return Int64.eq(p, Int64.ofInt(0));
	}
}

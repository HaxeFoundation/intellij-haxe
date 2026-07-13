package debug.values;

import debug.Pointer;
import debug.layout.Align;
import debug.layout.ObjectLayout;
import debug.target.MemoryReader;
import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;
import haxe.Int64;

/**
 * Lists the entries of a haxe.ds.BalancedTree (and its subclass
 * EnumValueMap) — a PURE-HAXE red/black tree, so unlike the native maps there
 * is no C layout to port: the wrapper's `root` field and each `TreeNode`'s
 * `left`/`right`/`key`/`value` fields are ordinary typed object fields read
 * through ObjectLayout. Produces an in-order (sorted) key → value listing.
 */
class TreeMapReader {
	static inline var MAX_ENTRIES = 512;
	static inline var MAX_DEPTH = 128; // guards against a cyclic/garbage tree

	final mem:MemoryReader;
	final align:Align;
	final objectLayout:ObjectLayout;
	final runtimeTypes:RuntimeTypes;

	public function new(mem:MemoryReader, align:Align, objectLayout:ObjectLayout, runtimeTypes:RuntimeTypes) {
		this.mem = mem;
		this.align = align;
		this.objectLayout = objectLayout;
		this.runtimeTypes = runtimeTypes;
	}

	/** True for BalancedTree and any subclass (EnumValueMap). */
	public static function isTreeMap(name:String):Bool {
		return name == "haxe.ds.EnumValueMap" || name == "haxe.ds.BalancedTree";
	}

	/** Live entry count (capped), or -1 when the tree can't be walked. */
	public function entryCount(mapPtr:Pointer, mapProto:ObjPrototype):Int {
		var root = rootNode(mapPtr, mapProto);
		if (root == null) {
			return rootPointer(mapPtr, mapProto).isNull() ? 0 : -1;
		}
		var counter = {n: 0};
		countRec(root.address, root.proto, counter, 0);
		return counter.n;
	}

	/**
	 * In-order entries. `keyValuePreview` renders a key or value at an address
	 * (both are HDyn-typed generic slots). valueAddress is read as HDyn by the
	 * caller. Capped at 512.
	 */
	public function entries(mapPtr:Pointer, mapProto:ObjPrototype, keyPreview:Pointer->String):Array<MapEntrySlot> {
		var root = rootNode(mapPtr, mapProto);
		var result:Array<MapEntrySlot> = [];
		if (root != null) {
			walkRec(root.address, root.proto, result, keyPreview, 0);
		}
		return result;
	}

	// --- tree walking ---

	function walkRec(node:Pointer, proto:ObjPrototype, out:Array<MapEntrySlot>, keyPreview:Pointer->String, depth:Int):Void {
		if (node.isNull() || depth > MAX_DEPTH || out.length >= MAX_ENTRIES) {
			return;
		}
		var fields = nodeFields(proto);
		walkRec(mem.readPointer(node.offset(fields.left)), proto, out, keyPreview, depth + 1);
		if (out.length >= MAX_ENTRIES) {
			return;
		}
		out.push({key: keyPreview(node.offset(fields.key)), valueAddress: node.offset(fields.value)});
		walkRec(mem.readPointer(node.offset(fields.right)), proto, out, keyPreview, depth + 1);
	}

	function countRec(node:Pointer, proto:ObjPrototype, counter:{n:Int}, depth:Int):Void {
		if (node.isNull() || depth > MAX_DEPTH || counter.n >= MAX_ENTRIES) {
			return;
		}
		var fields = nodeFields(proto);
		counter.n++;
		countRec(mem.readPointer(node.offset(fields.left)), proto, counter, depth + 1);
		countRec(mem.readPointer(node.offset(fields.right)), proto, counter, depth + 1);
	}

	// --- field offsets ---

	// the map's `root` field (inherited from BalancedTree) + the TreeNode proto
	function rootNode(mapPtr:Pointer, mapProto:ObjPrototype):Null<{address:Pointer, proto:ObjPrototype}> {
		var root = rootPointer(mapPtr, mapProto);
		if (root.isNull()) {
			return null;
		}
		var runtime = runtimeTypes.typeAt(mem.readPointer(root));
		var proto = switch (runtime) {
			case HObj(p) | HStruct(p): p;
			default: null;
		}
		return proto == null ? null : {address: root, proto: proto};
	}

	function rootPointer(mapPtr:Pointer, mapProto:ObjPrototype):Pointer {
		for (field in objectLayout.fields(mapProto)) {
			if (field.name == "root") {
				return mem.readPointer(mapPtr.offset(field.offset));
			}
		}
		return Int64.ofInt(0);
	}

	function nodeFields(proto:ObjPrototype):{left:Int, right:Int, key:Int, value:Int} {
		var left = 0, right = 0, key = 0, value = 0;
		for (field in objectLayout.fields(proto)) {
			switch (field.name) {
				case "left": left = field.offset;
				case "right": right = field.offset;
				case "key": key = field.offset;
				case "value": value = field.offset;
				default:
			}
		}
		return {left: left, right: right, key: key, value: value};
	}
}

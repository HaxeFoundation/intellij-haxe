package ijhaxe.debug.values;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.layout.ObjectLayout;
import ijhaxe.debug.target.MemoryReader;
import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;
import haxe.Int64;

/**
	Lists the entries of a haxe.ds.BalancedTree or its subclass EnumValueMap,
	sorted by key. Unlike the native maps, these are balanced trees written in
	Haxe, so they have no C layout: the map's `root` and each TreeNode's
	`left`, `right`, `key` and `value` are ordinary object fields, located
	through ObjectLayout.
**/
class TreeMapReader {
	static inline var MAX_ENTRIES = 512;
	static inline var MAX_DEPTH = 128; // guards against a cyclic or corrupt tree

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

	/**
		True for BalancedTree and any subclass (EnumValueMap).
	**/
	public static function isTreeMap(name:String):Bool {
		return name == "haxe.ds.EnumValueMap" || name == "haxe.ds.BalancedTree";
	}

	/**
		The number of entries, at most MAX_ENTRIES, or -1 when the tree cannot be walked.
	**/
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
		The entries sorted by key, at most MAX_ENTRIES of them. Keys and values
		are generic slots typed Dynamic: `keyPreview` renders the key at an
		address, and the caller reads each value address as Dynamic.
	**/
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

	// the node in the map's `root` field (declared by BalancedTree), with its runtime class
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

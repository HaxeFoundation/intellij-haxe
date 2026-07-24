package tests;

import ijhaxe.hxcpp.debug.values.Values;

private class Point {
	public var x:Int;
	public var y:Int;

	public function new(x:Int, y:Int) {
		this.x = x;
		this.y = y;
	}

	public function dist():Int {
		return x + y;
	}
}

private enum Shape {
	Circle(r:Int);
	Empty;
}

private class Labeled {
	public var id:Int;

	public function new(id:Int) {
		this.id = id;
	}

	public function toString():String {
		return "Labeled#" + id;
	}
}

private class MoodyLabel {
	public function new() {}

	public function toString():String {
		throw "no label";
	}
}

private class LoopyLabel {
	public function new() {}

	// non-tail self-recursion: on the eval target the overflow is a CATCHABLE
	// exception, so the label degrades gracefully — on real cpp it is fatal
	// to the process, which is why toString rendering defaults OFF there
	public function toString():String {
		return "x" + toString();
	}
}

// A property getter that records being called: rendering must NEVER invoke it
// (getters run user code on the server thread while the debuggee is paused —
// a lock-taking getter deadlocks the whole session).
private class Gated {
	public static var getterCalls = 0;

	public var plain:Int = 7;
	// @:isVar: the physical backing field IS listed by getInstanceFields, so a
	// getProperty-based renderer would invoke get_computed here
	@:isVar public var computed(get, set):Int = 13;

	public function new() {}

	function get_computed():Int {
		getterCalls++;
		return this.computed;
	}

	function set_computed(value:Int):Int {
		return this.computed = value;
	}
}

class ValuesTest {
	public static function run(assert:Assert):Void {
		primitivesAreLeaves(assert);
		arraysExpandToElements(assert);
		objectsExpandToDataFieldsOnly(assert);
		anonymousObjectsExpand(assert);
		enumsExpandToParameters(assert);
		renderingNeverInvokesGetters(assert);
		toStringLabelsAreOptIn(assert);
		toStringNeverRunsWithoutADeclaration(assert);
		throwingToStringDegradesToTheClassName(assert);
		recursingToStringDegradesWhereCatchable(assert);
		stringMapListsItsEntries(assert);
		intMapListsItsEntries(assert);
		objectMapKeysFollowTheDescribePolicy(assert);
		enumValueMapListsItsEntries(assert);
	}

	static function entry(entries:Array<{name:String, value:Dynamic}>, name:String):Dynamic {
		for (candidate in entries) {
			if (candidate.name == name) {
				return candidate.value;
			}
		}
		return null;
	}

	static function stringMapListsItsEntries(assert:Assert):Void {
		var map = new haxe.ds.StringMap<Int>();
		map.set("build", 92);
		map.set("name", 7);
		var described = Values.describe(map);
		assert.equals("Map(2)", described.value, "entry-count summary, not the raw hash handle");
		assert.isTrue(described.expandable, "a populated map expands");
		// with the opt-in ON the summary becomes the map's own content preview
		Values.renderWithToString = true;
		var preview = Values.describe(map).value;
		assert.isTrue(StringTools.startsWith(preview, "["), "content preview with the opt-in on: " + preview);
		assert.isTrue(preview.indexOf("build => 92") >= 0, "preview shows entries: " + preview);
		assert.isTrue(preview.indexOf("name => 7") >= 0, "preview shows every entry: " + preview);
		Values.renderWithToString = false;
		var entries = Values.children(map);
		assert.equals(2, entries.length, "one child per entry");
		assert.equals(92, entry(entries, '"build"'), "string keys are quoted like string values");
		assert.equals(7, entry(entries, '"name"'), "every entry is listed");
		assert.isTrue(!Values.describe(new haxe.ds.StringMap<Int>()).expandable, "an empty map is a leaf");
	}

	static function intMapListsItsEntries(assert:Assert):Void {
		var map = new haxe.ds.IntMap<String>();
		map.set(3, "three");
		assert.equals("Map(1)", Values.describe(map).value, "IntMap summary");
		assert.equals("three", entry(Values.children(map), "3"), "int keys are bare");
	}

	static function objectMapKeysFollowTheDescribePolicy(assert:Assert):Void {
		var map = new haxe.ds.ObjectMap<Labeled, Int>();
		var key = new Labeled(7);
		map.set(key, 42);
		assert.equals(42, entry(Values.children(map), className(key)),
			"off: an object key is named by its class, running no code");
		Values.renderWithToString = true;
		assert.equals(42, entry(Values.children(map), "Labeled#7"),
			"on: an object key follows the same toString policy as values");
		Values.renderWithToString = false;
	}

	static function enumValueMapListsItsEntries(assert:Assert):Void {
		// EnumValueMap extends BalancedTree — the isOfType guard must catch it
		var map = new haxe.ds.EnumValueMap<Shape, Int>();
		map.set(Empty, 1);
		map.set(Circle(5), 2);
		assert.equals("Map(2)", Values.describe(map).value, "tree-map summary");
		var entries = Values.children(map);
		assert.equals(1, entry(entries, "Empty"), "a paramless enum key by its constructor");
		assert.equals(2, entry(entries, "Circle(…)"), "an enum key with params by its constructor");
	}

	static function className(value:Dynamic):String {
		return Type.getClassName(Type.getClass(value));
	}

	static function toStringLabelsAreOptIn(assert:Assert):Void {
		var labeled = new Labeled(7);
		assert.equals(className(labeled), Values.describe(labeled).value,
			"off (the default): the class name, even with a declared toString");
		Values.renderWithToString = true;
		assert.equals("Labeled#7", Values.describe(labeled).value, "on: the object's own toString");
		Values.renderWithToString = false;
	}

	static function toStringNeverRunsWithoutADeclaration(assert:Assert):Void {
		Values.renderWithToString = true;
		var gated = new Gated();
		Gated.getterCalls = 0;
		var described = Values.describe(gated);
		assert.equals(0, Gated.getterCalls, "still no getter runs with the opt-in on");
		assert.equals(className(gated), described.value, "no declared toString: the class name, no code run");
		Values.renderWithToString = false;
	}

	static function throwingToStringDegradesToTheClassName(assert:Assert):Void {
		Values.renderWithToString = true;
		var moody = new MoodyLabel();
		assert.equals(className(moody), Values.describe(moody).value, "a throwing toString degrades");
		Values.renderWithToString = false;
	}

	static function recursingToStringDegradesWhereCatchable(assert:Assert):Void {
		// the eval target raises a CATCHABLE overflow for runaway recursion; on
		// real cpp the same toString kills the process (the reason the feature
		// defaults off) — this pins the graceful path where one exists
		Values.renderWithToString = true;
		var loopy = new LoopyLabel();
		assert.equals(className(loopy), Values.describe(loopy).value, "a self-recursing toString degrades");
		Values.renderWithToString = false;
	}

	static function renderingNeverInvokesGetters(assert:Assert):Void {
		var gated = new Gated();
		Gated.getterCalls = 0;
		Values.describe(gated);
		var kids = Values.children(gated);
		assert.equals(0, Gated.getterCalls, "describe/children ran NO getter");
		var names = [for (k in kids) k.name];
		assert.isTrue(names.indexOf("plain") >= 0, "plain field listed");
		for (kid in kids) {
			if (kid.name == "plain") {
				assert.equals(7, kid.value, "plain field read raw");
			}
		}
	}

	static function primitivesAreLeaves(assert:Assert):Void {
		assert.equals("42", Values.describe(42).value, "int value");
		assert.equals("Int", Values.describe(42).type, "int type");
		assert.isTrue(!Values.describe(42).expandable, "int is a leaf");
		assert.equals('"hi"', Values.describe("hi").value, "string is quoted");
		assert.isTrue(!Values.describe("hi").expandable, "string is a leaf");
		assert.equals("null", Values.describe(null).value, "null");
		assert.isTrue(!Values.describe(true).expandable, "bool is a leaf");
	}

	static function arraysExpandToElements(assert:Assert):Void {
		var arr = [10, 20, 30];
		var d = Values.describe(arr);
		assert.isTrue(d.expandable, "non-empty array expandable");
		assert.equals("Array (3)", d.value, "array summary");
		var kids = Values.children(arr);
		assert.equals(3, kids.length, "three elements");
		assert.equals("[1]", kids[1].name, "indexed name");
		assert.equals(20, kids[1].value, "element value");
		assert.isTrue(!Values.describe([]).expandable, "empty array is a leaf");
	}

	static function objectsExpandToDataFieldsOnly(assert:Assert):Void {
		var p = new Point(3, 4);
		assert.isTrue(Values.describe(p).expandable, "object expandable");
		var kids = Values.children(p);
		var names = [for (k in kids) k.name];
		assert.isTrue(names.indexOf("x") >= 0, "has field x");
		assert.isTrue(names.indexOf("y") >= 0, "has field y");
		assert.isTrue(names.indexOf("dist") < 0, "methods excluded");
	}

	static function anonymousObjectsExpand(assert:Assert):Void {
		var o = {a: 1, b: "two"};
		assert.isTrue(Values.describe(o).expandable, "anon expandable");
		assert.equals(2, Values.children(o).length, "two fields");
	}

	static function enumsExpandToParameters(assert:Assert):Void {
		assert.isTrue(Values.describe(Circle(5)).expandable, "enum with params expandable");
		assert.equals(5, Values.children(Circle(5))[0].value, "enum param value");
		assert.isTrue(!Values.describe(Empty).expandable, "paramless enum is a leaf");
	}
}

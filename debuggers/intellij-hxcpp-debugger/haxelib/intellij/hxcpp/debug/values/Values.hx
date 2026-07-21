package intellij.hxcpp.debug.values;

/**
	Renders and expands live debuggee values by ordinary reflection — the whole
	point of an IN-PROCESS server: a value is a real Haxe object, so
	`Type.typeof`/`Reflect` describe and walk it with no memory decoding. Pure
	and target-neutral, so it runs under the interpreter against plain values in
	the unit tests exactly as it does over real cpp values.

	SAFETY RULE: never run user code implicitly. Rendering happens on the
	server thread while the debuggee's threads are PAUSED — a property getter
	or toString that takes a lock held by a paused thread wedges the whole
	session (observed live). So fields are read RAW (`Reflect.field`, which
	never invokes a getter) and objects are labeled by class name, never
	stringified. Calling a getter is what `evaluate` is for — an explicit,
	user-initiated risk.
**/
class Values {
	/**
		Object labels via the object's own toString() — a DELIBERATE, opt-in
		exception to the safety rule above, controlled by the user through the
		custom `intellij/setToStringRendering` request (a project-level IDE
		setting, off by default, toggleable live from the Variables view).
		Only a class whose chain DECLARES toString ever runs code (the
		reflection probe itself calls nothing), and a THROWING toString
		degrades to the class name. What cannot be defended in-process: a
		STACK-OVERFLOWING toString (self-recursion, circular references) kills
		the debuggee before any handler runs — on hxcpp the overflow is a
		fatal 0xC00000FD, uncatchable by design (probe-verified). That risk is
		exactly why the default is OFF and turning it on is the user's call.
	**/
	public static var renderWithToString:Bool = false;

	/** Display string + type name + whether the value has expandable children. */
	public static function describe(value:Dynamic):{value:String, type:String, expandable:Bool} {
		return switch (Type.typeof(value)) {
			case TNull: {value: "null", type: "Unknown", expandable: false};
			case TInt: {value: Std.string(value), type: "Int", expandable: false};
			case TFloat: {value: Std.string(value), type: "Float", expandable: false};
			case TBool: {value: Std.string(value), type: "Bool", expandable: false};
			case TFunction: {value: "<function>", type: "Function", expandable: false};
			case TClass(c) if (c == String): {value: '"' + Std.string(value) + '"', type: "String", expandable: false};
			case TClass(c) if (c == Array):
				var arr:Array<Dynamic> = value;
				{value: "Array (" + arr.length + ")", type: "Array", expandable: arr.length > 0};
			// Maps BEFORE the generic object case: raw fields are the native
			// hash handle ("h = Dynamic"), useless to a user — render the entry
			// count and expand to the entries, like the HashLink adapter does.
			// std map iteration is library code, not user code (the safety rule
			// is about getters/toString), so listing entries is fair game.
			case TClass(c) if (c == haxe.ds.StringMap || c == haxe.ds.IntMap || c == haxe.ds.ObjectMap
					|| Std.isOfType(value, haxe.ds.BalancedTree)):
				// with the toString opt-in ON the summary is the map's own
				// content preview ("[build => 92, name => 7]", the std maps all
				// declare toString) — objectLabel applies the usual policy and
				// falls back to the entry count when off or on failure
				var count = mapEntries(value).length;
				{value: objectLabel(value, c, "Map(" + count + ")"), type: Type.getClassName(c), expandable: count > 0};
			case TClass(c):
				var name = Type.getClassName(c);
				{value: objectLabel(value, c, name), type: name, expandable: dataFields(value, c).length > 0};
			case TObject: {value: "{ }", type: "Anonymous", expandable: Reflect.fields(value).length > 0};
			case TEnum(e):
				var params = Type.enumParameters(value);
				var ctor = Type.enumConstructor(value);
				{value: params.length > 0 ? ctor + "(…)" : ctor, type: Type.getEnumName(e), expandable: params.length > 0};
			case TUnknown: {value: Std.string(value), type: "Unknown", expandable: false};
		};
	}

	/** The named/indexed children of an expandable value (empty for a leaf). */
	public static function children(value:Dynamic):Array<{name:String, value:Dynamic}> {
		return switch (Type.typeof(value)) {
			case TClass(c) if (c == String): [];
			case TClass(c) if (c == Array):
				var arr:Array<Dynamic> = value;
				[for (i in 0...arr.length) {name: "[" + i + "]", value: arr[i]}];
			case TClass(c) if (c == haxe.ds.StringMap || c == haxe.ds.IntMap || c == haxe.ds.ObjectMap
					|| Std.isOfType(value, haxe.ds.BalancedTree)):
				mapEntries(value);
			case TClass(c):
				[for (f in dataFields(value, c)) {name: f, value: rawField(value, f)}];
			case TObject:
				[for (f in Reflect.fields(value)) {name: f, value: Reflect.field(value, f)}];
			case TEnum(_):
				var params = Type.enumParameters(value);
				[for (i in 0...params.length) {name: "[" + i + "]", value: params[i]}];
			default: [];
		};
	}

	// The entries of any haxe.ds map (StringMap/IntMap/ObjectMap, and
	// BalancedTree covering EnumValueMap), one child per entry. Entry names:
	// string keys quoted (like string VALUES render), int keys bare, and
	// object/enum keys named by the same describe() policy as values — so an
	// object key follows the user's toString opt-in and an enum key shows its
	// constructor. All maps share the keys()/get() iteration surface.
	static function mapEntries(value:Dynamic):Array<{name:String, value:Dynamic}> {
		var entries:Array<{name:String, value:Dynamic}> = [];
		var keys:Iterator<Dynamic> = value.keys();
		for (key in keys) {
			var name = Std.isOfType(key, String) ? '"' + key + '"'
				: Std.isOfType(key, Int) ? Std.string(key)
				: describe(key).value;
			entries.push({name: name, value: value.get(key)});
		}
		return entries;
	}

	// The object's display label: its own toString() result when the user
	// opted in AND the class chain declares one; the class name otherwise.
	// getInstanceFields includes inherited fields, so one probe covers the
	// whole chain without running anything.
	static function objectLabel(value:Dynamic, c:Class<Dynamic>, className:String):String {
		if (!renderWithToString || Type.getInstanceFields(c).indexOf("toString") == -1) {
			return className; // no opt-in, or no user toString: never run code
		}
		return try {
			var text:String = value.toString();
			(text == null || text.length == 0) ? className : truncate(text);
		} catch (e:Dynamic) {
			className; // a throwing toString degrades to the class name
		}
	}

	// A value LABEL is a one-line summary; a runaway toString (a big map's
	// content preview, a verbose user render) must not flood the wire or the
	// tree row. 200 chars comfortably fills the Variables column.
	static inline var MAX_LABEL = 200;

	static function truncate(text:String):String {
		return text.length <= MAX_LABEL ? text : text.substr(0, MAX_LABEL - 1) + "…";
	}

	// Instance fields that hold data (not methods) — the ones a user inspects.
	// Raw reads only: getProperty would run every getter of every object in
	// scope just to LIST locals (see the class doc's safety rule).
	static function dataFields(value:Dynamic, c:Class<Dynamic>):Array<String> {
		return [for (f in Type.getInstanceFields(c)) if (!Reflect.isFunction(rawField(value, f))) f];
	}

	// Reflect.field never invokes a getter; a computed (non-physical) property
	// simply doesn't appear, which is the safe rendering of it.
	static function rawField(value:Dynamic, name:String):Dynamic {
		return try Reflect.field(value, name) catch (e:Dynamic) null;
	}
}

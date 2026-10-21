package ijhaxe.hxcpp.debug.values;

/**
	Renders and expands live debuggee values by ordinary reflection. The
	server runs inside the debuggee, so a value is a real Haxe object that
	`Type.typeof` and `Reflect` can describe and walk without decoding memory.
	The code is target-neutral, so the unit tests run it under the
	interpreter over plain values, exactly as it runs over real cpp values.

	SAFETY RULE: never run user code implicitly. Rendering runs on the server
	thread while the debuggee's threads are PAUSED. A property getter or a
	toString that takes a lock held by a paused thread hangs the whole
	session. Fields are therefore read RAW (`Reflect.field` never invokes a
	getter), and objects are labeled by class name, never stringified.
	Running a getter is what `evaluate` is for: an explicit risk the user
	chooses.
**/
class Values {
	/**
		Labels objects with their own toString(). This is a DELIBERATE opt-in
		exception to the safety rule above. The user controls it through the
		custom `custom/setToStringRendering` request, backed by a project-level
		IDE setting that is off by default and can be toggled from the
		Variables view. Code runs only for a class whose chain DECLARES
		toString; checking for it calls nothing. A THROWING toString falls back
		to the class name. One risk cannot be contained inside the process: a
		toString that overflows the stack (self-recursion, circular
		references) kills the debuggee before any handler runs. On hxcpp the
		overflow is a fatal 0xC00000FD that cannot be caught. That risk is why
		the default is OFF and turning it on is the user's decision.
	**/
	public static var renderWithToString:Bool = false;

	/** The display string, the type name, and whether the value has children to expand. */
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
			// Maps come BEFORE the generic object case. Their raw field is the
			// native hash handle ("h = Dynamic"), which is useless to a user,
			// so a map shows its entry count and expands to its entries, as in
			// the HashLink adapter. Iterating a std map runs library code, not
			// user code, so the safety rule (getters, toString) allows it.
			case TClass(c) if (c == haxe.ds.StringMap || c == haxe.ds.IntMap || c == haxe.ds.ObjectMap
					|| Std.isOfType(value, haxe.ds.BalancedTree)):
				// With the toString opt-in ON, the summary is the map's own
				// content preview ("[build => 92, name => 7]"); every std map
				// declares toString. objectLabel applies the usual policy and
				// falls back to the entry count when the opt-in is off or
				// toString fails.
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

	// The entries of any haxe.ds map (StringMap, IntMap, ObjectMap, and
	// BalancedTree, which covers EnumValueMap), one child per entry. All of
	// them offer keys() and get(). A string key is quoted like a string
	// value, an int key is shown bare, and an object or enum key is named
	// like a value by describe(). An object key thus follows the user's
	// toString opt-in, and an enum key shows its constructor.
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
	// opted in AND its class chain declares one, else the class name.
	// getInstanceFields includes inherited fields, so one lookup covers the
	// whole chain without running any code.
	static function objectLabel(value:Dynamic, c:Class<Dynamic>, className:String):String {
		if (!renderWithToString || Type.getInstanceFields(c).indexOf("toString") == -1) {
			return className; // no opt-in, or no user toString: never run code
		}
		return try {
			var text:String = value.toString();
			(text == null || text.length == 0) ? className : truncate(text);
		} catch (e:Dynamic) {
			className; // a throwing toString falls back to the class name
		}
	}

	// A value LABEL is a one-line summary. A very long toString result (a big
	// map's content preview, a verbose user rendering) must not flood the
	// connection or the tree row; 200 characters fill the Variables column.
	static inline var MAX_LABEL_LENGTH = 200;

	static function truncate(text:String):String {
		return text.length <= MAX_LABEL_LENGTH ? text : text.substr(0, MAX_LABEL_LENGTH - 1) + "…";
	}

	// The instance fields that hold data rather than methods, the ones a user
	// inspects. Only raw reads: getProperty would run every getter of every
	// object in scope just to LIST the locals (see the safety rule).
	static function dataFields(value:Dynamic, c:Class<Dynamic>):Array<String> {
		return [for (f in Type.getInstanceFields(c)) if (!Reflect.isFunction(rawField(value, f))) f];
	}

	// Reflect.field never invokes a getter. A computed property without a
	// backing field therefore does not appear, which is the safe way to show it.
	static function rawField(value:Dynamic, name:String):Dynamic {
		return try Reflect.field(value, name) catch (e:Dynamic) null;
	}
}

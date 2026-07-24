package ijhaxe.hxcpp.debug.values;

import ijhaxe.dap.protocol.Variable;
import ijhaxe.hxcpp.debug.DebuggerApi;

/** What a variablesReference expands to. */
private enum Container {
	Frame(thread:Int, frame:Int); // a stack frame's locals
	ObjectValue(value:Dynamic); // an expandable value's children
}

/**
	Owns the DAP variablesReference registry and the scopes/variables/setVariable
	surface. A reference names either a frame's locals or an expandable value;
	`reset()` clears the registry on every resume, because a reference must never
	outlive the stop that created it (the debuggee moves on).

	Values are rendered/expanded by Values (pure reflection), so the whole class
	is exercised under the interpreter over a fake DebuggerApi returning plain
	Haxe values.
**/
class VariablesView {
	final debugger:DebuggerApi;
	final registry:Map<Int, Container> = new Map();
	var nextReference:Int = 1; // 0 means "no children" in DAP

	public function new(debugger:DebuggerApi) {
		this.debugger = debugger;
	}

	/** Invalidates every reference (called on resume). */
	public function reset():Void {
		registry.clear();
		nextReference = 1;
	}

	/** A reference for `frame`'s locals, for the "Locals" scope. */
	public function frameScope(thread:Int, frame:Int):Int {
		return register(Frame(thread, frame));
	}

	/** The variables of a reference (a frame's locals or a value's children). */
	public function variables(reference:Int):Array<Variable> {
		var container = registry.get(reference);
		if (container == null) {
			return [];
		}
		return switch (container) {
			case Frame(thread, frame):
				[for (name in debugger.stackVariables(thread, frame)) safeVariable(name, () -> debugger.stackVariableValue(thread, frame, name))];
			case ObjectValue(value):
				[for (child in Values.children(value)) safeVariable(child.name, () -> child.value)];
		};
	}

	// One corrupt value slot (real frames hold raw pointers and half-built
	// state; hxcpp re-raises a critical read error as a THROW on the debug
	// thread) must poison ONE row, not the whole request — render the error
	// text in the value column and keep going.
	function safeVariable(name:String, read:() -> Dynamic):Variable {
		return try {
			variable(name, read());
		} catch (e:Dynamic) {
			{name: name, value: "<unreadable: " + Std.string(e) + ">", type: "Unknown", variablesReference: 0};
		}
	}

	/**
		Writes `raw` (a DAP text value) to `name` inside `reference`'s container
		and returns the DAP result. Frame locals write to ANY frame via the
		runtime; a value's field writes through reflection.
	**/
	public function setVariable(reference:Int, name:String, raw:String):Null<{value:String, type:String, variablesReference:Int}> {
		var container = registry.get(reference);
		if (container == null) {
			return null;
		}
		var parsed = parseValue(raw);
		var stored:Dynamic = switch (container) {
			case Frame(thread, frame):
				debugger.setStackVariableValue(thread, frame, name, parsed);
			case ObjectValue(value):
				Reflect.setProperty(value, name, parsed);
				parsed;
		};
		var described = Values.describe(stored);
		return {
			value: described.value,
			type: described.type,
			variablesReference: described.expandable ? register(ObjectValue(stored)) : 0
		};
	}

	/**
		Renders a standalone value (an evaluate result) with a child reference
		when expandable — reusing the same registry so it can be drilled into.
	**/
	public function present(value:Dynamic):{value:String, type:String, variablesReference:Int} {
		var described = Values.describe(value);
		return {
			value: described.value,
			type: described.type,
			variablesReference: described.expandable ? register(ObjectValue(value)) : 0
		};
	}

	function variable(name:String, value:Dynamic):Variable {
		var described = Values.describe(value);
		return {
			name: name,
			value: described.value,
			type: described.type,
			variablesReference: described.expandable ? register(ObjectValue(value)) : 0
		};
	}

	function register(container:Container):Int {
		var reference = nextReference++;
		registry.set(reference, container);
		return reference;
	}

	// A DAP text value -> a Haxe value: bool/int/float/quoted-string literals,
	// else the raw string. (Constructing objects is out of scope.)
	static function parseValue(raw:String):Dynamic {
		var trimmed = StringTools.trim(raw);
		if (trimmed == "true") return true;
		if (trimmed == "false") return false;
		if (trimmed == "null") return null;
		if (trimmed.length >= 2 && StringTools.startsWith(trimmed, '"') && StringTools.endsWith(trimmed, '"')) {
			return trimmed.substr(1, trimmed.length - 2);
		}
		var asInt = Std.parseInt(trimmed);
		if (asInt != null && Std.string(asInt) == trimmed) {
			return asInt;
		}
		var asFloat = Std.parseFloat(trimmed);
		if (!Math.isNaN(asFloat)) {
			return asFloat;
		}
		return raw;
	}
}

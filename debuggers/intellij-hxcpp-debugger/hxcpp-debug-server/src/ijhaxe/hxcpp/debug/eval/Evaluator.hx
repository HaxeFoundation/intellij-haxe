package ijhaxe.hxcpp.debug.eval;

import hscript.Expr;
import hscript.Interp;
import hscript.Parser;
import hscript.Tools;
import ijhaxe.hxcpp.debug.DebuggerApi;

/**
	Evaluates watch, hover and breakpoint-condition expressions against a
	stopped frame.

	The server runs inside the debuggee, so hscript can parse the expression
	and evaluate it by ordinary reflection over the real values. Operators,
	comparisons, field access, indexing and method calls therefore all work
	without a custom interpreter. The frame's locals (and `this`) are copied
	into hscript's variable scope. A bare `ident = expr` assignment is written
	back through the runtime, so it persists in the debuggee.

	It is plain Haxe, so the unit tests run it under the interpreter.
**/
class Evaluator {
	final debugger:DebuggerApi;
	final parser = new Parser();

	public function new(debugger:DebuggerApi) {
		this.debugger = debugger;
	}

	/**
		Evaluates `expression` in `frame` of `thread` and returns the resulting
		value. A top-level `name = value` writes back to the frame (and returns
		the stored value). Throws a String message on a parse/eval error.
	**/
	public function evaluate(thread:Int, frame:Int, expression:String):Dynamic {
		var trimmed = StringTools.trim(expression);
		var assignment = topLevelAssignment(trimmed);

		var interp = new ResolvingInterp();
		for (name in debugger.stackVariables(thread, frame)) {
			// A corrupt slot is skipped rather than failing the whole
			// expression; only an expression that USES it fails, with
			// EUnknownVariable.
			try {
				interp.variables.set(name, debugger.stackVariableValue(thread, frame, name));
			} catch (e:Dynamic) {}
		}

		var source = assignment != null ? assignment.rhs : trimmed;
		var program = try {
			parser.parseString(source);
		} catch (e:Dynamic) {
			throw "Cannot parse expression: " + Std.string(e);
		}
		interp.bindTypePaths(program);
		var value = try {
			interp.execute(program);
		} catch (e:Dynamic) {
			throw Std.string(e);
		}

		if (assignment != null) {
			// persist to the debuggee frame, not just hscript's scratch scope
			return debugger.setStackVariableValue(thread, frame, assignment.name, value);
		}
		return value;
	}

	/**
		Evaluates `condition` as a Bool for a conditional breakpoint. It fails
		safe: any error (a bad expression, a non-Bool result, a missing local)
		returns true, so a broken condition stops at the breakpoint instead of
		silently skipping it.
	**/
	public function conditionHolds(thread:Int, frame:Int, condition:String):Bool {
		return try {
			var result = evaluate(thread, frame, condition);
			// only a real Bool decides; anything else fails safe (stops)
			Std.isOfType(result, Bool) ? (result : Bool) : true;
		} catch (e:Dynamic) {
			true;
		}
	}

	// Splits `name = rhs`, where `name` is a bare identifier and `=` is not
	// part of `==`, `<=` or the like. Null when the expression is not such an
	// assignment.
	static function topLevelAssignment(source:String):Null<{name:String, rhs:String}> {
		var eq = findTopLevelAssign(source);
		if (eq < 0) {
			return null;
		}
		var name = StringTools.trim(source.substr(0, eq));
		if (!isIdentifier(name)) {
			return null;
		}
		return {name: name, rhs: StringTools.trim(source.substr(eq + 1))};
	}

	// The index of the first top-level `=` that is a plain assignment (not
	// ==, !=, <=, >=), ignoring anything inside brackets, parentheses and
	// strings; -1 if there is none.
	static function findTopLevelAssign(source:String):Int {
		var depth = 0;
		var inString = false;
		var quote = 0;
		var i = 0;

		while (i < source.length) {
			var c = source.charCodeAt(i);
			if (inString) {
				if (c == quote) inString = false;
			} else if (c == "'".code || c == '"'.code) {
				inString = true;
				quote = c;
			} else if (c == "(".code || c == "[".code) {
				depth++;
			} else if (c == ")".code || c == "]".code) {
				depth--;
			} else if (depth == 0 && c == "=".code) {
				var prev = i > 0 ? source.charCodeAt(i - 1) : 0;
				var next = i + 1 < source.length ? source.charCodeAt(i + 1) : 0;
				var comparison = next == "=".code || prev == "=".code
					|| prev == "!".code || prev == "<".code || prev == ">".code;
				if (!comparison) {
					return i;
				}
			}
			i++;
		}
		return -1;
	}

	static function isIdentifier(s:String):Bool {
		if (s.length == 0) {
			return false;
		}
		for (i in 0...s.length) {
			var c = s.charCodeAt(i);
			var ok = (c >= "a".code && c <= "z".code) || (c >= "A".code && c <= "Z".code)
				|| c == "_".code || (i > 0 && c >= "0".code && c <= "9".code);
			if (!ok) {
				return false;
			}
		}
		return true;
	}
}

/**
	An hscript `Interp` whose identifier lookup falls back to the debuggee's
	own types, so expressions can call static methods and construct objects
	(`Counter.bump(5)`, `my.pack.Target.fn(x)`, `new Point(1, 2)`).

	hscript is not a sandbox: method calls run through `Reflect.callMethod` on
	the REAL object. An evaluated call therefore executes compiled debuggee
	code, and its side effects persist in the program.

	A type name takes one of two shapes in an expression, and each has its
	own mechanism:

	- A bare identifier (`Math`, `Std`, the root-package `Counter`) resolves
	  at execution time through the `resolve` override.
	- A dotted path (`my.pack.Target.fn`) parses as field access on the free
	  identifier `my`. `bindTypePaths` therefore scans the parsed program
	  first and binds each dotted prefix that names a type into `variables`,
	  as nested anonymous objects (`my` → `{pack: {Target: cls}}`). Binding
	  happens BEFORE execution and only for paths that resolve, so unknown
	  identifiers still raise errors, and a broken breakpoint condition
	  still fails safe.

	`new my.pack.Target(...)` needs neither: hscript keeps the full dotted
	path in `ENew`, and `Interp.cnew` resolves it directly.
**/
private class ResolvingInterp extends Interp {
	// The package objects created by bindTypePaths, so that chains sharing a
	// root (`a.b.X` and `a.c.Y`) merge instead of overwriting each other.
	final packageRoots = new Map<String, Dynamic>();

	/**
		Bypasses `Interp.exprReturn`. On hxcpp, a catch typed to one enum
		catches ANY enum, so exprReturn's `catch(e:Stop)` also catches
		hscript's own `Error` enum. Its switch matches no `Stop` case and falls
		through to `return null`. Every runtime error (unknown identifier, null
		access) would then evaluate to null on the native target, while the
		interpreter throws correctly. Calling `expr` directly lets errors
		propagate. The `Stop` values that a top-level `return` or `break`
		throws are handled here by name, because `Stop` is private to its
		hscript module and cannot be caught by type.
	**/
	override public function execute(program:Expr):Dynamic {
		depth = 0;
		locals = new Map();
		declared = new Array();

		try {
			return expr(program);
		} catch (e:Dynamic) {
			switch (Std.string(e)) {
				case "SReturn":
					var v = returnValue;
					returnValue = null;
					return v;
				case "SBreak":
					throw "Invalid break";
				case "SContinue":
					throw "Invalid continue";
				default:
					throw e;
			}
		}
	}

	override function resolve(id:String):Dynamic {
		if (variables.exists(id)) {
			return variables.get(id);
		}
		var cls = Type.resolveClass(id);
		if (cls != null) {
			return cls;
		}
		var en = Type.resolveEnum(id);
		if (en != null) {
			return en;
		}
		return super.resolve(id); // throws EUnknownVariable
	}

	/**
		Walks the parsed program and binds every dotted type path in advance
		(see the class doc). Call it after the frame locals are in `variables`,
		because locals shadow packages, and before `execute`.
	**/
	public function bindTypePaths(program:Expr):Void {
		switch (Tools.expr(program)) {
			case EField(_, _):
				tryBindChain(program);
			default:
		}
		Tools.iter(program, bindTypePaths);
	}

	// Binds the longest dotted prefix of an `a.b.c.d` chain that names a class
	// or enum, if there is one. `chain` is the chain's outermost EField.
	function tryBindChain(chain:Expr):Void {
		var segments = new Array<String>();
		var current = chain;
		while (true) {
			switch (Tools.expr(current)) {
				case EField(inner, field):
					segments.unshift(field);
					current = inner;
				case EIdent(id):
					segments.unshift(id);
					break;
				default:
					return; // rooted in an expression, not a free identifier
			}
		}
		// A frame local, or anything else already bound, owns the root name.
		// Only the package objects created here may be extended.
		if (variables.exists(segments[0]) && !packageRoots.exists(segments[0])) {
			return;
		}
		// longest prefix first: `a.b.Cls` must win over a package `a.b`
		var length = segments.length;
		while (length >= 2) {
			var path = segments.slice(0, length).join(".");
			var type:Dynamic = Type.resolveClass(path);
			if (type == null) {
				type = Type.resolveEnum(path);
			}
			if (type != null) {
				bindPath(segments, length, type);
				return;
			}
			length--;
		}
	}

	// binds `a.b.Cls` as variables["a"] = {b: {Cls: type}}
	function bindPath(segments:Array<String>, length:Int, type:Dynamic):Void {
		var root = packageRoots.get(segments[0]);
		if (root == null) {
			root = {};
			packageRoots.set(segments[0], root);
			variables.set(segments[0], root);
		}
		var node:Dynamic = root;
		for (i in 1...length - 1) {
			var next:Dynamic = Reflect.field(node, segments[i]);
			if (next == null) {
				next = {};
				Reflect.setField(node, segments[i], next);
			}
			node = next;
		}
		Reflect.setField(node, segments[length - 1], type);
	}
}

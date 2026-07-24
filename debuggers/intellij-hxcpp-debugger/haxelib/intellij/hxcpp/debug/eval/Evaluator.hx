package intellij.hxcpp.debug.eval;

import hscript.Expr;
import hscript.Interp;
import hscript.Parser;
import hscript.Tools;
import intellij.hxcpp.debug.DebuggerApi;

/**
	Evaluates watch/hover/condition expressions against a stopped frame.

	In-process again pays off: hscript parses the expression and evaluates it by
	ordinary reflection, so operators, comparisons, field access, indexing and
	method calls all work with no interpreter of our own. The frame's locals
	(and `this`) are bridged into hscript's variable scope; a bare `ident = expr`
	assignment is routed back to the runtime so it persists in the debuggee.

	Pure Haxe, so it runs under the interpreter in the unit tests.
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
			// a corrupt slot must not break the whole expression: skip it (the
			// expression then fails with EUnknownVariable only if it USES it)
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
		Evaluates `condition` as a Bool for a conditional breakpoint. FAIL SAFE:
		any error (bad expression, non-Bool, missing local) returns true, so a
		broken condition stops rather than silently swallowing the breakpoint —
		the caller surfaces the reason.
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

	// `name = rhs` where `name` is a bare identifier (not `==`, `<=`, etc.).
	// Returns null when the expression is not such an assignment.
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

	// Index of a top-level `=` that is a plain assignment (not ==, !=, <=, >=),
	// ignoring anything inside brackets/parens/strings; -1 if none.
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
	hscript `Interp` whose identifier lookup falls back to the debuggee's own
	types, so expressions can call static methods and construct objects
	(`Counter.bump(5)`, `my.pack.Target.fn(x)`, `new Point(1, 2)`).

	Method calls run through `Reflect.callMethod` on the REAL object — hscript
	is not a sandbox — so an evaluated call executes compiled debuggee code and
	its side effects persist in the program.

	Two mechanisms cover the two shapes a type name takes in an expression:

	- a bare identifier (`Math`, `Std`, root-package `Counter`) resolves at
	  execution time via the `resolve` override;
	- a dotted path (`my.pack.Target.fn`) parses as field access on the free
	  identifier `my`, so `bindTypePaths` pre-scans the parsed program and
	  binds each resolvable dotted prefix into `variables` as nested anonymous
	  objects (`my` → `{pack: {Target: cls}}`). Binding BEFORE execution (and
	  only paths that actually resolve) keeps unknown-identifier errors — and
	  with them the conditional-breakpoint fail-safe — intact.

	`new my.pack.Target(...)` needs neither: hscript keeps the full dotted
	path in `ENew` and `Interp.cnew` resolves it directly.
**/
private class ResolvingInterp extends Interp {
	// package objects created by bindTypePaths, so chains sharing a root
	// (`a.b.X` and `a.c.Y`) merge instead of clobbering each other
	final packageRoots = new Map<String, Dynamic>();

	/**
		Bypasses `Interp.exprReturn`. On hxcpp, catching by enum type catches
		ANY enum, so exprReturn's `catch(e:Stop)` also swallows hscript's own
		`Error` enum: the switch matches no `Stop` case and falls through to
		`return null` — every runtime error (unknown identifier, null access)
		silently evaluated to null on the native target while correctly
		throwing under the interpreter. Calling `expr` directly lets errors
		propagate; the `Stop` control-flow enums a top-level `return`/`break`
		would throw are re-handled here by name (`Stop` itself is
		module-private to hscript, so it cannot be caught by type).
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
		Walks the parsed program and pre-binds every dotted type path (see the
		class doc). Call after the frame locals are in `variables` — locals
		shadow packages — and before `execute`.
	**/
	public function bindTypePaths(program:Expr):Void {
		switch (Tools.expr(program)) {
			case EField(_, _):
				tryBindChain(program);
			default:
		}
		Tools.iter(program, bindTypePaths);
	}

	// `program` is the outermost EField of an `a.b.c.d` chain; binds the
	// longest dotted prefix that names a class or enum, if any
	function tryBindChain(program:Expr):Void {
		var segments = new Array<String>();
		var current = program;
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
		// a frame local (or anything else already bound) owns the root name;
		// our own package objects are the one thing safe to extend
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

	// materializes `a.b.Cls` as variables["a"] = {b: {Cls: type}}
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

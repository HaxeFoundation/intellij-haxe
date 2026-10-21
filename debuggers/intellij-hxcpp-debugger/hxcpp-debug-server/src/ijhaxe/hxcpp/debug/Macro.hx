package ijhaxe.hxcpp.debug;

import haxe.macro.Compiler;
import haxe.macro.Context;
import haxe.macro.Expr;
#if macro
import haxe.macro.Type;
#end

/**
	Compile-time entry points of the debug server library.

	extraParams.hxml calls `injectServer`, so adding
	`-lib intellij-hxcpp-debug-server` to a `-debug` cpp build is all a user
	does. The macro pulls the Server class into the build (it starts itself
	from its static init) and defines `HXCPP_DEBUGGER`, which turns on the
	hxcpp runtime's debugger support (checked throws, breakpoint hooks).

	It also registers `bakeLineTable`, which records the executable-line
	table (see `breakpoints.LineTable`). "Baking" means computing the table at
	compile time and storing it in the binary. hxcpp has no line table at
	runtime, so the only reliable source of "which lines have code" is the
	typed AST of this very compilation: the same positions gencpp turns into
	HXLINE markers.
**/
class Macro {
	// Must equal LineTable.RESOURCE_NAME. It is a copy rather than a reference
	// so that neither the initialization macro nor its onGenerate callback has
	// to load a runtime type; haxe 5 restricts what initialization macros may
	// touch.
	static inline var LINE_TABLE_RESOURCE = "ijhaxe.hxcpp.debug.lineTable";

	public static function injectServer():Void {
		#if macro
		if (Context.defined("cpp") && Context.defined("debug") && !Context.defined("display")) {
			// Define FIRST: the whole Server class sits inside #if HXCPP_DEBUGGER,
			// so loading the type before the define finds an empty module.
			Compiler.define("HXCPP_DEBUGGER");

			// Force Server, which starts itself from its static init, into the
			// build. haxe 5 forbids Context.getType in an initialization macro,
			// so the load is deferred to onAfterInitMacros; the define above is
			// already set by then. onAfterInitMacros exists only on 4.3+, and
			// 4.1/4.2 still allow the direct call.
			#if (haxe_ver >= 4.3)
			Context.onAfterInitMacros(() -> Context.getType("ijhaxe.hxcpp.debug.Server"));
			#else
			Context.getType("ijhaxe.hxcpp.debug.Server");
			#end

			Context.onGenerate(bakeLineTable);
		}
		#end
	}

	/**
		The value of the compile-time define `key`, or `fallback` when it is
		not defined. Server connection settings come from environment
		variables first, then from these defines, then from the defaults.
	**/
	macro public static function definedValue(key:String, fallback:Expr):Expr {
		var value = Context.definedValue(key);
		return value == null ? fallback : macro $v{value};
	}

	#if macro
	/**
		Collects the start line of every typed expression that reaches code
		generation and stores a `file|l1,l2,...` table as a resource. onGenerate
		runs after dead-code elimination, so these are exactly the expressions
		gencpp instruments. The server reads the table back through LineTable
		to verify breakpoint lines.
	**/
	static function bakeLineTable(types:Array<Type>):Void {
		// file -> set of expression-start BYTE offsets. Positions repeat a lot,
		// so they are deduplicated before the conversion to lines.
		var offsetsByFile = new Map<String, Map<Int, Bool>>();
		for (type in types) {
			switch (type) {
				case TInst(ref, _):
					var cls = ref.get();
					if (cls.isExtern) {
						continue;
					}
					for (field in cls.fields.get()) {
						collectField(field, offsetsByFile);
					}
					for (field in cls.statics.get()) {
						collectField(field, offsetsByFile);
					}
					if (cls.constructor != null) {
						collectField(cls.constructor.get(), offsetsByFile);
					}
				default:
			}
		}
		var buf = new StringBuf();
		for (file in offsetsByFile.keys()) {
			var lines = offsetsToLines(file, [for (offset in offsetsByFile.get(file).keys()) offset]);
			if (lines.length == 0) {
				continue;
			}
			buf.add(file);
			buf.add("|");
			buf.add(lines.join(","));
			buf.add("\n");
		}
		Context.addResource(LINE_TABLE_RESOURCE, haxe.io.Bytes.ofString(buf.toString()));
	}

	static function collectField(field:ClassField, offsetsByFile:Map<String, Map<Int, Bool>>):Void {
		var expr = field.expr();
		if (expr != null) {
			collectExpr(expr, offsetsByFile);
		}
	}

	static function collectExpr(expr:TypedExpr, offsetsByFile:Map<String, Map<Int, Bool>>):Void {
		// Before haxe 4.2, a TFunction's position starts in the comments and
		// whitespace BEFORE the declaration, so a leading comment line would
		// enter the table and a breakpoint there would falsely verify. It is
		// skipped there; the body carries the function's real lines. From 4.2
		// the position starts at the declaration itself, a line hxcpp
		// instruments, so it is recorded.
		#if (haxe_ver < 4.2)
		var skip = expr.expr.match(TFunction(_));
		#else
		var skip = false;
		#end

		var pos = Context.getPosInfos(expr.pos);
		if (!skip && pos.min >= 0 && pos.file != null && pos.file.length > 0) {
			var offsets = offsetsByFile.get(pos.file);
			if (offsets == null) {
				offsets = new Map();
				offsetsByFile.set(pos.file, offsets);
			}
			offsets.set(pos.min, true);
		}
		haxe.macro.TypedExprTools.iter(expr, e -> collectExpr(e, offsetsByFile));
	}

	// Converts byte offsets to sorted, unique 1-based line numbers in a single
	// pass over the file. The file is read as BYTES because positions are
	// byte offsets, which avoids any assumption about the string encoding. An
	// unreadable file (a macro-generated position) contributes no lines.
	static function offsetsToLines(file:String, offsets:Array<Int>):Array<Int> {
		var bytes = try sys.io.File.getBytes(file) catch (e:Dynamic) null;
		if (bytes == null) {
			return [];
		}
		offsets.sort((a, b) -> a - b);
		var lines:Array<Int> = [];
		var line = 1;
		var at = 0;
		for (offset in offsets) {
			if (offset >= bytes.length) {
				break;
			}
			while (at < offset) {
				if (bytes.get(at) == 0x0A) {
					line++;
				}
				at++;
			}
			if (lines.length == 0 || lines[lines.length - 1] != line) {
				lines.push(line);
			}
		}
		return lines;
	}
	#end
}

package intellij.hxcpp.debug;

import haxe.macro.Compiler;
import haxe.macro.Context;
import haxe.macro.Expr;
#if macro
import haxe.macro.Type;
#end

/**
	Build-time entry points of the debug server library.

	`injectServer` is wired through extraParams.hxml, so adding
	`-lib intellij-hxcpp-debug-server` to a `-debug` cpp build is all a user
	does: the server class is pulled into the build (it starts itself from its
	static init) and `HXCPP_DEBUGGER` is defined, which turns on the hxcpp
	runtime's debugger support (checked throws, breakpoint hooks).

	It also registers the `onGenerate` walk that bakes the executable-line
	table (see `breakpoints.LineTable`): hxcpp has no runtime line table, so
	the only reliable "which lines have code" source is the typed AST of this
	very compilation — the same positions gencpp turns into HXLINE markers.
**/
class Macro {
	// Must equal LineTable.RESOURCE_NAME. Duplicated (not referenced) so the
	// init/onGenerate macro context never has to load a runtime type — haxe 5
	// restricts what initialization macros may touch.
	static inline var LINE_TABLE_RESOURCE = "intellij.hxcpp.debug.lineTable";

	public static function injectServer():Void {
		#if macro
		if (Context.defined("cpp") && Context.defined("debug") && !Context.defined("display")) {
			// define FIRST: Server's whole class is #if HXCPP_DEBUGGER guarded,
			// so pulling the type in before the define finds an empty module
			Compiler.define("HXCPP_DEBUGGER");
			// force Server (which self-starts from its static init) into the build.
			// haxe 5 forbids Context.getType from an initialization macro, so defer
			// it to onAfterInitMacros there — the define above is already set, so the
			// deferred load still sees an HXCPP_DEBUGGER-enabled module. onAfterInitMacros
			// only exists on 4.3+, and the direct call is still allowed on 4.1/4.2.
			#if (haxe_ver >= 4.3)
			Context.onAfterInitMacros(() -> Context.getType("intellij.hxcpp.debug.Server"));
			#else
			Context.getType("intellij.hxcpp.debug.Server");
			#end
			Context.onGenerate(bakeLineTable);
		}
		#end
	}

	/**
		The value of compile-time define `key`, or `fallback` when absent — the
		middle link of the env-var -> define -> default configuration chain.
	**/
	macro public static function definedValue(key:String, fallback:Expr):Expr {
		var value = Context.definedValue(key);
		return value == null ? fallback : macro $v{value};
	}

	#if macro
	/**
		Collects the start line of every typed expression that survives to
		generation (onGenerate runs after DCE — exactly what gencpp will
		instrument) and bakes a `file|l1,l2,...` table as a resource. The
		server reads it back through LineTable to verify breakpoint lines.
	**/
	static function bakeLineTable(types:Array<Type>):Void {
		// file -> set of expression-start BYTE offsets (dedup before the
		// offset->line conversion; positions repeat heavily)
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
		var pos = Context.getPosInfos(expr.pos);
		if (pos.min >= 0 && pos.file != null && pos.file.length > 0) {
			var offsets = offsetsByFile.get(pos.file);
			if (offsets == null) {
				offsets = new Map();
				offsetsByFile.set(pos.file, offsets);
			}
			offsets.set(pos.min, true);
		}
		haxe.macro.TypedExprTools.iter(expr, e -> collectExpr(e, offsetsByFile));
	}

	// Byte offsets -> sorted unique 1-based line numbers, one single pass over
	// the file per file. Read as BYTES because positions are byte offsets — no
	// string-encoding assumptions. An unreadable file (macro-generated
	// positions) simply contributes no lines.
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

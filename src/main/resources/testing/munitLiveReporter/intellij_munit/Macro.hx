package intellij_munit;

#if macro
import haxe.macro.Compiler;
import haxe.macro.Context;
import haxe.macro.Expr;

/**
	Entry point of the IDE's munit test reporting, applied to the test compile
	as `--macro intellij_munit.Macro.init()`. Registers a build macro on
	`massive.munit.TestRunner` that appends an `addResultClient` call to the
	constructor, so per-test TeamCity events stream on stdout alongside
	whatever clients the project's own TestMain installed. munit has no
	TeamCity reporter of its own, so this client IS the IDE's result channel.

	Reporting failure modes degrade to a plain run instead of breaking the
	build: global metadata on an absent type is inert (a build without munit
	compiles untouched), and a munit without `addResultClient` gets a warning
	and unmodified fields. The SINGLE-TEST filter is the exception - when it
	cannot be proven sound the compile fails instead, because an unsound
	filter registers no tests and the run would pass empty.
**/
class Macro {
	public static function init():Void {
		Compiler.addGlobalMetadata("massive.munit.TestRunner", "@:build(intellij_munit.Macro.buildRunner())", false, true, false);
		// single-test gutter runs: munit's runtime collects @Test methods by
		// reflection with no filter hook, so the collection itself is patched
		// to register only the selected method
		if (Context.definedValue("intellij_munit_test") != null) {
			Compiler.addGlobalMetadata("massive.munit.TestClassHelper", "@:build(intellij_munit.Macro.buildClassHelper())", false, true, false);
		}
	}

	public static function buildClassHelper():Array<Field> {
		var fields = Context.getBuildFields();
		var selected = Context.definedValue("intellij_munit_test");
		for (field in fields) {
			if (field.name != "addTest") continue;
			switch (field.kind) {
				case FFun(fn) if (fn.expr != null && fn.args.length > 0):
					// addTest(field, ...) is the one choke point every test
					// registers through; guarding it narrows the run exactly.
					// The guard compares the FIRST argument against the selected
					// name, which is only sound while that argument is the test
					// name String - against anything else the comparison would
					// register NO tests and the run would pass empty.
					if (!isStringType(fn.args[0].type)) {
						Context.error("IDE single-test run cannot filter: this munit version's "
							+ "TestClassHelper.addTest does not take the test name String as its "
							+ "first argument. Run the whole suite instead.", Context.currentPos());
						return fields;
					}
					var nameArg = fn.args[0].name;
					fn.expr = macro {
						if ($i{nameArg} != $v{selected}) return;
						${fn.expr};
					};
					return fields;
				default:
			}
		}
		// a hard stop, not a warning: an unpatched collection would run every
		// test under a run the user asked to be ONE test
		Context.error("IDE single-test run could not patch massive.munit.TestClassHelper.addTest. "
			+ "Run the whole suite instead.", Context.currentPos());
		return fields;
	}

	static function isStringType(type:Null<ComplexType>):Bool {
		return switch (type) {
			case TPath({name: "String", pack: []}): true;
			default: false;
		}
	}

	public static function buildRunner():Array<Field> {
		var fields = Context.getBuildFields();
		if (!hasField(fields, "addResultClient")) {
			Context.warning("IDE test reporting needs munit's addResultClient; "
				+ "this munit version has none - results stay console-only", Context.currentPos());
			return fields;
		}
		for (field in fields) {
			if (field.name != "new") continue;
			switch (field.kind) {
				case FFun(fn) if (fn.expr != null):
					var suiteName = Context.definedValue("teamcity_suite_name");
					var rootSuite = suiteName == null ? "" : suiteName;
					fn.expr = macro {
						${fn.expr};
						this.addResultClient(new intellij_munit.LiveClient($v{rootSuite}));
					};
					return fields;
				default:
			}
		}
		Context.warning("IDE test reporting could not patch massive.munit.TestRunner's constructor - results stay console-only",
			Context.currentPos());
		return fields;
	}

	static function hasField(fields:Array<Field>, name:String):Bool {
		for (field in fields) {
			if (field.name == name) return true;
		}
		return false;
	}
}
#end

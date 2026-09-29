package intellij_utest;

#if macro
import haxe.macro.Compiler;
import haxe.macro.Context;
import haxe.macro.Expr;

/**
	Entry point of the IDE's live test reporting, applied to the test compile
	as `--macro intellij_utest.Macro.init()`. Registers a build macro on
	`utest.Runner` that appends a `LiveReporter.attach(this, ...)` call to the
	constructor, so per-test events stream on stdout while utest's own batch
	reporter (still active through `-D teamcity`) remains the fallback the IDE
	deduplicates against.

	Every failure mode degrades to the batch behavior instead of breaking the
	build: global metadata on an absent type is inert (a build without utest
	compiles untouched), and the build macro verifies the runner's capability
	before patching - a utest without the `onTestStart` dispatcher gets a
	warning and unmodified fields.
**/
class Macro {
	public static function init():Void {
		Compiler.addGlobalMetadata("utest.Runner", "@:build(intellij_utest.Macro.buildRunner())", false, true, false);
	}

	public static function buildRunner():Array<Field> {
		var fields = Context.getBuildFields();
		if (!hasField(fields, "onTestStart") || !hasField(fields, "onTestComplete")) {
			Context.warning("Live test reporting needs utest's onTestStart/onTestComplete dispatchers; "
				+ "this utest version has none - batch reporting stays", Context.currentPos());
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
						intellij_utest.LiveReporter.attach(this, $v{rootSuite});
					};
					return fields;
				default:
			}
		}
		Context.warning("Live test reporting could not patch utest.Runner's constructor - batch reporting stays",
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

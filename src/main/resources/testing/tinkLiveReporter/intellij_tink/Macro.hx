package intellij_tink;

#if macro
import haxe.macro.Compiler;
import haxe.macro.Context;
import haxe.macro.Expr;

/**
	Entry point of the IDE's tink_unittest test reporting, applied to the test
	compile as `--macro intellij_tink.Macro.init()`. Registers a build macro
	on `tink.testrunner.Runner` that makes the IDE's TeamCity reporter the
	DEFAULT of `Runner.run`'s optional reporter argument — a TestMain calling
	`Runner.run(batch)` gets it without any code change, and one passing its
	own reporter keeps it. tink has no TeamCity reporter of its own, so this
	is the IDE's result channel.

	Every failure mode degrades to a plain run instead of breaking the build:
	global metadata on an absent type is inert (a build without tink_testrunner
	compiles untouched), and the build macro verifies the run signature before
	patching - an unrecognized runner gets a warning and unmodified fields.
**/
class Macro {
	public static function init():Void {
		Compiler.addGlobalMetadata("tink.testrunner.Runner", "@:build(intellij_tink.Macro.buildRunner())", false, true, false);
	}

	public static function buildRunner():Array<Field> {
		var fields = Context.getBuildFields();
		for (field in fields) {
			if (field.name != "run") continue;
			switch (field.kind) {
				case FFun(fn) if (fn.expr != null && fn.args.length >= 2 && fn.args[1].name == "reporter"):
					var suiteName = Context.definedValue("teamcity_suite_name");
					var rootSuite = suiteName == null ? "" : suiteName;
					fn.expr = macro {
						if (reporter == null) reporter = new intellij_tink.TcReporter($v{rootSuite});
						${fn.expr};
					};
					return fields;
				default:
			}
		}
		Context.warning("IDE test reporting could not patch tink.testrunner.Runner.run - results stay console-only",
			Context.currentPos());
		return fields;
	}
}
#end

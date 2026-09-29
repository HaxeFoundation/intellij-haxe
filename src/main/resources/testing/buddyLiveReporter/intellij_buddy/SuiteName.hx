package intellij_buddy;

/** Inlines the -D teamcity_suite_name value into the reporter at compile time, because runtime code cannot read defines. */
class SuiteName {
	public static macro function defined():haxe.macro.Expr {
		var value = haxe.macro.Context.definedValue("teamcity_suite_name");
		return macro $v{value == null ? "" : value};
	}
}

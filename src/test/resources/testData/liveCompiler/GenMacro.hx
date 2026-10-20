import haxe.macro.Context;

// Defines gen.GeneratedThing - a type that exists in no source file, only in
// the compiler's post-macro world. The live-integration test verifies the IDE
// discovers, completes and resolves it.
class GenMacro {
	public static function define() {
		// a warm compilation server re-runs init macros against its cache -
		// redefining an already-cached type is an error, so guard like
		// well-behaved macro libraries do
		try {
			Context.getType("gen.GeneratedThing");
			return;
		} catch (e:Dynamic) {}
		Context.defineType({
			pack: ["gen"],
			name: "GeneratedThing",
			pos: Context.currentPos(),
			kind: TDClass(),
			fields: [{
				name: "tag",
				access: [APublic],
				kind: FVar(macro :String, macro "gen"),
				pos: Context.currentPos()
			}, {
				name: "make",
				access: [APublic, AStatic],
				kind: FFun({args: [], ret: macro :String, expr: macro return "made"}),
				pos: Context.currentPos()
			}]
		});
	}
}

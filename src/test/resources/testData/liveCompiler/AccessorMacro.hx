import haxe.macro.Context;
import haxe.macro.Expr;

// Adds a getter for every @:accessor field. A getter takes its field's
// position, so the compiler places the generated member on the source field.
class AccessorMacro {
	public static function build():Array<Field> {
		var fields = Context.getBuildFields();
		var getters:Array<Field> = [];
		for (field in fields) {
			if (field.meta == null || !Lambda.exists(field.meta, entry -> entry.name == ":accessor")) continue;
			switch (field.kind) {
				case FVar(type, _):
					var name = field.name;
					getters.push({
						name: "get" + name.charAt(0).toUpperCase() + name.substr(1),
						access: [APublic],
						pos: field.pos,
						kind: FFun({args: [], ret: type, expr: macro return $i{name}})
					});
				default:
			}
		}
		return fields.concat(getters);
	}
}

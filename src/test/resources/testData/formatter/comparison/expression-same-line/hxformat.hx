class Main {
	static function parse(text:String):Int {
		return Std.parseInt(text);
	}

	static function main() {
		var mode = "dev";
		var level = if (mode == "dev") 1 else 2;
		var label = try Std.string(level) catch (e:Dynamic) "?";
		var big = switch (mode) {
			case "dev": 10;
			default: 20;
		};
		var kind = if (level > 1) "large" else "small";
		var parsed = try parse(mode) catch (e:Dynamic) 0;
		var a = if (level > 1) 1 else 2;
		var b = try parse(mode) catch (e:Dynamic) -1;
		var c = switch (mode) {
			case "dev": 10;
			default: 0;
		}
		trace(level + big + label + kind + parsed + a + b + c);
	}
}
